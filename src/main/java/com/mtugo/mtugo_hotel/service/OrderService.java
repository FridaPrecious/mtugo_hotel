package com.mtugo.mtugo_hotel.service;

import com.mtugo.mtugo_hotel.dto.CartDTO;
import com.mtugo.mtugo_hotel.dto.CartItemDTO;
import com.mtugo.mtugo_hotel.dto.OrderRequest;
import com.mtugo.mtugo_hotel.dto.OrderResponse;
import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.OrderAudit;
import com.mtugo.mtugo_hotel.entity.OrderStatus;
import com.mtugo.mtugo_hotel.entity.PaymentLog;
import com.mtugo.mtugo_hotel.entity.Transaction;
import com.mtugo.mtugo_hotel.entity.TransactionStatus;
import com.mtugo.mtugo_hotel.repository.MealRepository;
import com.mtugo.mtugo_hotel.repository.OrderAuditRepository;
import com.mtugo.mtugo_hotel.repository.OrderRepository;
import com.mtugo.mtugo_hotel.repository.PaymentLogRepository;
import com.mtugo.mtugo_hotel.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MealRepository mealRepository;
    private final TransactionRepository transactionRepository;
    private final OrderAuditRepository orderAuditRepository;
    private final PaymentLogRepository paymentLogRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        MealRepository mealRepository,
                        TransactionRepository transactionRepository,
                        OrderAuditRepository orderAuditRepository,
                        PaymentLogRepository paymentLogRepository) {
        this.orderRepository = orderRepository;
        this.mealRepository = mealRepository;
        this.transactionRepository = transactionRepository;
        this.orderAuditRepository = orderAuditRepository;
        this.paymentLogRepository = paymentLogRepository;
    }

    /**
     * Writes one row to order_audit every time an order's status changes, so
     * there's a permanent trail of who moved an order and when - useful for
     * disputes ("staff marked this ready at 12:41") and for debugging stuck
     * orders.
     */
    private void recordAudit(Order order, OrderStatus previousStatus, OrderStatus newStatus, String changedBy) {
        OrderAudit audit = new OrderAudit();
        audit.setOrder(order);
        audit.setPreviousStatus(previousStatus == null ? null : previousStatus.name());
        audit.setNewStatus(newStatus.name());
        audit.setChangedBy(changedBy);
        orderAuditRepository.save(audit);
    }

    /**
     * Writes one row to payment_logs for a Transaction lifecycle event, so the
     * M-Pesa side of an order (STK push sent, callback received, failure
     * reason) is traceable independently of the application log files.
     */
    private void recordPaymentLog(Transaction transaction, PaymentLog.LogType type, String message) {
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setTransaction(transaction);
        paymentLog.setLogType(type);
        paymentLog.setMessage(message);
        paymentLogRepository.save(paymentLog);
    }

    /**
     * Used by the staff dashboard to move an order forward in its lifecycle.
     * Centralizes the valid-transition check (instead of leaving it to the
     * controller) and records an order_audit row tagged as STAFF.
     */
    @Transactional
    public Order updateOrderStatusByStaff(Long orderId, OrderStatus newStatus) {
        Order order = findOrderById(orderId);
        OrderStatus currentStatus = order.getStatus();

        boolean isValidTransition = switch (currentStatus) {
            case PAID -> newStatus == OrderStatus.PREPARING;
            case PREPARING -> newStatus == OrderStatus.READY;
            case READY -> newStatus == OrderStatus.COMPLETED;
            default -> false;
        };

        if (!isValidTransition) {
            throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        recordAudit(updated, currentStatus, newStatus, "STAFF");

        recalculateActiveQueueETAs();
        log.info("Order {} status updated to {} by staff", orderId, newStatus);
        return updated;
    }

    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order - mealId: {}, quantity: {}, phone: {}",
                request.getMealId(), request.getQuantity(), request.getPhone());

        Meal meal = mealRepository.findById(request.getMealId())
                .orElseThrow(() -> {
                    log.error("Meal not found with id: {}", request.getMealId());
                    return new RuntimeException("Meal not found with id: " + request.getMealId());
                });

        log.info("Found meal: {} (price: {})", meal.getName(), meal.getPrice());

        Double totalAmount = meal.getPrice() * request.getQuantity();
        log.info("Total amount calculated: {}", totalAmount);

        Order order = new Order();
        order.setMeal(meal);
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(totalAmount);
        order.setCustomerPhone(request.getPhone());
        order.setStatus(OrderStatus.PENDING);
        order.setRequestedPickupTime(request.getPickupTime());

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with id: {}", savedOrder.getId());

        return OrderResponse.builder()
                .orderId(savedOrder.getId())
                .totalAmount(savedOrder.getTotalAmount())
                .status(savedOrder.getStatus().name())
                .message("Order created successfully")
                .build();
    }

    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found with id: {}", orderId);
                    return new RuntimeException("Order not found with id: " + orderId);
                });
    }

    @Transactional
    public void updateOrderCheckoutRequestId(Long orderId, String checkoutRequestId) {
        Order order = findOrderById(orderId);
        order.setCheckoutRequestId(checkoutRequestId);
        orderRepository.save(order);

        for (Order sibling : findGroupSiblings(order)) {
            sibling.setCheckoutRequestId(checkoutRequestId);
            orderRepository.save(sibling);
        }

        log.info("Updated order {} with CheckoutRequestID: {}", orderId, checkoutRequestId);
    }

    @Transactional
    public void markOrderAsPaid(Long orderId, String mpesaReceiptNumber) {
        Order order = findOrderById(orderId);
        List<Order> group = findGroupIncludingSelf(order);

        LocalDateTime paidAt = LocalDateTime.now();
        for (Order o : group) {
            OrderStatus previousStatus = o.getStatus();
            o.setStatus(OrderStatus.PAID);
            o.setPaidAt(paidAt);
            orderRepository.save(o);
            recordAudit(o, previousStatus, OrderStatus.PAID, "SYSTEM");
        }

        recalculateActiveQueueETAs();
        log.info("Order {} (and {} sibling item(s)) marked as PAID", orderId, group.size() - 1);
    }

    /**
     * Recomputes Expected Ready Time for every order currently PAID or
     * PREPARING, per staff-requirements.md AC-8:
     *
     *   ETA = NOW + SUM(prep_time of all PAID orders)
     *             + SUM(prep_time of all PREPARING orders)
     *
     * Call this whenever the active queue changes - a new order is paid, or
     * an order moves PREPARING -> READY (freeing up kitchen capacity) - since
     * every remaining order's ETA shifts as a result.
     *
     * Orders from the same cart checkout share a single pickup, so within a
     * cart group the latest ETA is applied to every item in that group. A
     * customer's requested pickup time is honored if it's later than the
     * queue-driven ETA.
     */
    @Transactional
    public void recalculateActiveQueueETAs() {
        List<Order> paidOrders = orderRepository.findByStatusOrderByOrderTimeAsc(OrderStatus.PAID);
        List<Order> preparingOrders = orderRepository.findByStatusOrderByPaidAtAsc(OrderStatus.PREPARING);

        int totalPrepMinutes = 0;
        for (Order o : paidOrders) {
            totalPrepMinutes += o.getMeal().getPrepTimeMinutes();
        }
        for (Order o : preparingOrders) {
            totalPrepMinutes += o.getMeal().getPrepTimeMinutes();
        }

        LocalDateTime queueEta = LocalDateTime.now().plusMinutes(totalPrepMinutes);

        List<Order> active = new java.util.ArrayList<>(paidOrders);
        active.addAll(preparingOrders);

        for (Order order : active) {
            LocalDateTime eta = queueEta;
            if (order.getRequestedPickupTime() != null && order.getRequestedPickupTime().isAfter(eta)) {
                eta = order.getRequestedPickupTime();
            }
            order.setExpectedReadyAt(eta);
            orderRepository.save(order);
        }

        // Keep every item in a cart group showing the same pickup time.
        active.stream()
                .filter(o -> o.getCartGroupId() != null)
                .collect(java.util.stream.Collectors.groupingBy(Order::getCartGroupId))
                .forEach((groupId, groupOrders) -> {
                    LocalDateTime latest = groupOrders.stream()
                            .map(Order::getExpectedReadyAt)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);
                    if (latest == null) return;
                    for (Order o : groupOrders) {
                        if (!latest.equals(o.getExpectedReadyAt())) {
                            o.setExpectedReadyAt(latest);
                            orderRepository.save(o);
                        }
                    }
                });

        log.info("Recalculated queue ETAs - {} active order(s), total prep time {} min, ETA {}",
                active.size(), totalPrepMinutes, queueEta);
    }

    @Transactional
    public void markOrderAsFailed(Long orderId) {
        Order order = findOrderById(orderId);
        for (Order o : findGroupIncludingSelf(order)) {
            OrderStatus previousStatus = o.getStatus();
            o.setStatus(OrderStatus.FAILED);
            orderRepository.save(o);
            recordAudit(o, previousStatus, OrderStatus.FAILED, "SYSTEM");
        }
        log.info("Order {} (and any sibling cart items) marked as FAILED", orderId);
    }

    @Transactional
    public Transaction createTransaction(Long orderId, String phoneNumber, Double amount) {
        Order order = findOrderById(orderId);
        Transaction transaction = new Transaction();
        transaction.setOrder(order);
        transaction.setAmount(amount);
        transaction.setPhoneNumber(phoneNumber);
        transaction.setStatus(TransactionStatus.INITIATED);
        Transaction saved = transactionRepository.save(transaction);
        recordPaymentLog(saved, PaymentLog.LogType.INFO,
                "Transaction created for order " + orderId + ", phone " + phoneNumber);
        return saved;
    }

    @Transactional
    public void updateTransactionCheckoutRequestId(Long transactionId, String checkoutRequestId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        transaction.setCheckoutRequestId(checkoutRequestId);
        transaction.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        recordPaymentLog(transaction, PaymentLog.LogType.INFO,
                "STK Push sent to Safaricom - CheckoutRequestID " + checkoutRequestId);
        log.info("Transaction {} updated with CheckoutRequestID: {}", transactionId, checkoutRequestId);
    }

    @Transactional
    public void completeTransactionAndOrder(Long transactionId, String mpesaReceiptNumber, String resultDesc) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setMpesaReceiptNumber(mpesaReceiptNumber);
        transaction.setResultDescription(resultDesc);
        transactionRepository.save(transaction);
        recordPaymentLog(transaction, PaymentLog.LogType.INFO,
                "Payment confirmed - receipt " + mpesaReceiptNumber + " - " + resultDesc);

        markOrderAsPaid(transaction.getOrder().getId(), mpesaReceiptNumber);
    }

    @Transactional
    public void failTransactionAndOrder(Long transactionId, String resultDesc) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setResultDescription(resultDesc);
        transactionRepository.save(transaction);
        recordPaymentLog(transaction, PaymentLog.LogType.ERROR, "Payment failed - " + resultDesc);

        markOrderAsFailed(transaction.getOrder().getId());
    }

    /**
     * Returns the other orders sharing this order's cart group (not including itself).
     * Empty list if the order wasn't part of a multi-item cart checkout.
     */
    private List<Order> findGroupSiblings(Order order) {
        if (order.getCartGroupId() == null) {
            return List.of();
        }
        return orderRepository.findByCartGroupId(order.getCartGroupId()).stream()
                .filter(o -> !o.getId().equals(order.getId()))
                .toList();
    }

    /**
     * Returns every order in this order's cart group, including itself.
     * A single-item list if the order wasn't part of a multi-item cart checkout.
     */
    public List<Order> findGroupIncludingSelf(Order order) {
        if (order.getCartGroupId() == null) {
            return List.of(order);
        }
        return orderRepository.findByCartGroupId(order.getCartGroupId());
    }

    /**
     * Creates one Order per cart item, tagged with a shared cartGroupId so they're
     * paid for together (single STK push) and progressed together by staff.
     */
    @Transactional
    public List<Order> createOrderFromCart(CartDTO cart, String customerPhone, LocalDateTime pickupTime) {
        List<CartItemDTO> items = cart.getItems();
        if (items.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        String groupId = items.size() > 1 ? UUID.randomUUID().toString() : null;

        List<Order> created = new java.util.ArrayList<>();
        for (CartItemDTO item : items) {
            Meal meal = mealRepository.findById(item.getMealId())
                    .orElseThrow(() -> new RuntimeException("Meal not found: " + item.getMealId()));

            Order order = new Order();
            order.setMeal(meal);
            order.setQuantity(item.getQuantity());
            order.setTotalAmount(item.getSubtotal());
            order.setCustomerPhone(customerPhone);
            order.setStatus(OrderStatus.PENDING);
            order.setRequestedPickupTime(pickupTime);
            order.setCartGroupId(groupId);

            created.add(orderRepository.save(order));
        }
        return created;
    }
}