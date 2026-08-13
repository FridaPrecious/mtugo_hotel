package com.mtugo.mtugo_hotel.service;

import com.mtugo.mtugo_hotel.dto.CartDTO;
import com.mtugo.mtugo_hotel.dto.CartItemDTO;
import com.mtugo.mtugo_hotel.dto.OrderRequest;
import com.mtugo.mtugo_hotel.dto.OrderResponse;
import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.OrderStatus;
import com.mtugo.mtugo_hotel.entity.Transaction;
import com.mtugo.mtugo_hotel.entity.TransactionStatus;
import com.mtugo.mtugo_hotel.repository.MealRepository;
import com.mtugo.mtugo_hotel.repository.OrderRepository;
import com.mtugo.mtugo_hotel.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MealRepository mealRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        MealRepository mealRepository,
                        TransactionRepository transactionRepository) {
        this.orderRepository = orderRepository;
        this.mealRepository = mealRepository;
        this.transactionRepository = transactionRepository;
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
        log.info("Updated order {} with CheckoutRequestID: {}", orderId, checkoutRequestId);
    }

    @Transactional
    public void markOrderAsPaid(Long orderId, String mpesaReceiptNumber) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());

        int prepTime = order.getMeal().getPrepTimeMinutes();
        long pendingCount = orderRepository.countByStatusInAndIdNot(
                List.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.PREPARING),
                orderId
        );
        int buffer = (int) pendingCount * 2;
        LocalDateTime earliestReady = LocalDateTime.now().plusMinutes(prepTime + buffer);

        LocalDateTime eta = earliestReady;
        if (order.getRequestedPickupTime() != null && order.getRequestedPickupTime().isAfter(earliestReady)) {
            // Customer asked for a later pickup time than the kitchen needs - honor it.
            eta = order.getRequestedPickupTime();
        }
        order.setExpectedReadyAt(eta);

        orderRepository.save(order);
        log.info("Order {} marked as PAID. ETA: {}", orderId, eta);
    }

    @Transactional
    public void markOrderAsFailed(Long orderId) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        log.info("Order {} marked as FAILED", orderId);
    }

    @Transactional
    public Transaction createTransaction(Long orderId, String phoneNumber, Double amount) {
        Order order = findOrderById(orderId);
        Transaction transaction = new Transaction();
        transaction.setOrder(order);
        transaction.setAmount(amount);
        transaction.setPhoneNumber(phoneNumber);
        transaction.setStatus(TransactionStatus.INITIATED);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public void updateTransactionCheckoutRequestId(Long transactionId, String checkoutRequestId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        transaction.setCheckoutRequestId(checkoutRequestId);
        transaction.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(transaction);
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

        markOrderAsPaid(transaction.getOrder().getId(), mpesaReceiptNumber);
    }

    @Transactional
    public void failTransactionAndOrder(Long transactionId, String resultDesc) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setResultDescription(resultDesc);
        transactionRepository.save(transaction);

        markOrderAsFailed(transaction.getOrder().getId());
    }

    /**
     * Creates orders from a cart (one order per cart item).
     * Returns the first order for simplicity.
     * In production, use OrderItems to combine into a single order.
     */
    public Order createOrderFromCart(CartDTO cart, String customerPhone) {
        List<CartItemDTO> items = cart.getItems();
        if (items.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order firstOrder = null;
        for (CartItemDTO item : items) {
            Meal meal = mealRepository.findById(item.getMealId())
                    .orElseThrow(() -> new RuntimeException("Meal not found: " + item.getMealId()));

            Order order = new Order();
            order.setMeal(meal);
            order.setQuantity(item.getQuantity());
            order.setTotalAmount(item.getSubtotal());
            order.setCustomerPhone(customerPhone);
            order.setStatus(OrderStatus.PENDING);

            Order saved = orderRepository.save(order);
            if (firstOrder == null) {
                firstOrder = saved;
            }
        }
        return firstOrder;
    }
}