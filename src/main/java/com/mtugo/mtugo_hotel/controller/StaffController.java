package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.StaffDashboardResponse;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.OrderStatus;
import com.mtugo.mtugo_hotel.service.OrderService;
import com.mtugo.mtugo_hotel.service.StaffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private static final Logger log = LoggerFactory.getLogger(StaffController.class);

    private final StaffService staffService;
    private final OrderService orderService;

    @Autowired
    public StaffController(StaffService staffService, OrderService orderService) {
        this.staffService = staffService;
        this.orderService = orderService;
    }

    /**
     * GET /api/staff/orders/active
     * Returns all active orders (PAID, PREPARING, READY) grouped by status
     */
    @GetMapping("/orders/active")
    public ResponseEntity<StaffDashboardResponse> getActiveOrders() {
        StaffDashboardResponse response = staffService.getActiveOrders();
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/staff/orders/{orderId}/status
     * Updates the status of an order (PAID → PREPARING → READY → COMPLETED)
     */
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {

        log.info("Updating order {} status to {}", orderId, status);

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }

        Order order = orderService.findOrderById(orderId);
        OrderStatus currentStatus = order.getStatus();

        // Validate forward transition
        boolean isValidTransition = switch (currentStatus) {
            case PAID -> newStatus == OrderStatus.PREPARING;
            case PREPARING -> newStatus == OrderStatus.READY;
            case READY -> newStatus == OrderStatus.COMPLETED;
            default -> false;
        };

        if (!isValidTransition) {
            throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        // Update status
        if (newStatus == OrderStatus.COMPLETED) {
            // Just mark as completed
            order.setStatus(newStatus);
        } else if (newStatus == OrderStatus.READY) {
            order.setStatus(newStatus);
            // When marked as ready, the expected ready time should be now or in the past
        } else {
            order.setStatus(newStatus);
        }

        Order updated = orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, newStatus);

        return ResponseEntity.ok(updated);
    }

    // Inject orderRepository for the update method
    @Autowired
    private com.mtugo.mtugo_hotel.repository.OrderRepository orderRepository;
}