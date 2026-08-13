package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.Transaction;
import com.mtugo.mtugo_hotel.entity.TransactionStatus;
import com.mtugo.mtugo_hotel.repository.OrderRepository;
import com.mtugo.mtugo_hotel.repository.TransactionRepository;
import com.mtugo.mtugo_hotel.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PaymentStatusController {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusController.class);

    private final TransactionRepository transactionRepository;
    private final OrderService orderService;

    @Autowired
    public PaymentStatusController(TransactionRepository transactionRepository, OrderService orderService) {
        this.transactionRepository = transactionRepository;
        this.orderService = orderService;
    }

    /**
     * Show the payment status page
     */
    @GetMapping("/payment/status")
    public String paymentStatusPage(@RequestParam("checkoutRequestId") String checkoutRequestId, Model model) {
        log.info("Payment status page requested for checkoutRequestId: {}", checkoutRequestId);
        model.addAttribute("checkoutRequestId", checkoutRequestId);
        return "payment-status";
    }

    /**
     * Polling endpoint for frontend to check payment status
     */
    @GetMapping("/api/payment/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@RequestParam("checkoutRequestId") String checkoutRequestId) {
        log.debug("Checking payment status for checkoutRequestId: {}", checkoutRequestId);

        Map<String, Object> response = new HashMap<>();
        response.put("checkoutRequestId", checkoutRequestId);

        try {
            // Find transaction by checkoutRequestId
            Transaction transaction = transactionRepository.findByCheckoutRequestId(checkoutRequestId)
                    .orElse(null);

            if (transaction == null) {
                response.put("status", "NOT_FOUND");
                response.put("message", "Transaction not found");
                return ResponseEntity.ok(response);
            }

            Order order = transaction.getOrder();

            response.put("status", transaction.getStatus().name());
            response.put("orderId", order.getId());
            response.put("orderStatus", order.getStatus().name());
            response.put("totalAmount", order.getTotalAmount());
            response.put("mealName", order.getMeal().getName());

            if (transaction.getStatus() == TransactionStatus.COMPLETED) {
                response.put("mpesaReceiptNumber", transaction.getMpesaReceiptNumber());
                response.put("expectedReadyAt", order.getExpectedReadyAt());
                response.put("message", "Payment completed successfully!");
            } else if (transaction.getStatus() == TransactionStatus.FAILED) {
                response.put("message", "Payment failed: " + transaction.getResultDescription());
            } else {
                response.put("message", "Payment is being processed...");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking payment status", e);
            response.put("status", "ERROR");
            response.put("message", "Error checking payment status: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Lightweight polling endpoint for the customer-facing success page,
     * so customers can see live status once staff move the order along.
     */
    @GetMapping("/api/orders/{orderId}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable Long orderId) {
        Order order = orderService.findOrderById(orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("status", order.getStatus().name());
        response.put("mealName", order.getMeal().getName());
        response.put("quantity", order.getQuantity());
        response.put("expectedReadyAt", order.getExpectedReadyAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Success page after payment is confirmed
     */
    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam("orderId") Long orderId, Model model) {
        log.info("Payment success page requested for orderId: {}", orderId);
        Order order = orderService.findOrderById(orderId);
        List<Order> group = orderService.findGroupIncludingSelf(order);
        double groupTotal = group.stream().mapToDouble(Order::getTotalAmount).sum();

        model.addAttribute("order", order);
        model.addAttribute("meal", order.getMeal());
        model.addAttribute("orderGroup", group);
        model.addAttribute("groupTotal", groupTotal);
        return "payment-success";
    }

    /**
     * Failure page after payment failed
     */
    @GetMapping("/payment/failure")
    public String paymentFailure(@RequestParam("orderId") Long orderId, Model model) {
        log.info("Payment failure page requested for orderId: {}", orderId);
        Order order = orderService.findOrderById(orderId);
        model.addAttribute("order", order);
        return "payment-failure";
    }
}