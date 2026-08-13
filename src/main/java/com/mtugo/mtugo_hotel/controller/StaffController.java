package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.MealRequest;
import com.mtugo.mtugo_hotel.dto.StaffDashboardResponse;
import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.OrderStatus;
import com.mtugo.mtugo_hotel.service.MealService;
import com.mtugo.mtugo_hotel.service.OrderService;
import com.mtugo.mtugo_hotel.service.StaffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private static final Logger log = LoggerFactory.getLogger(StaffController.class);

    private final StaffService staffService;
    private final OrderService orderService;
    private final MealService mealService;

    @Autowired
    public StaffController(StaffService staffService, OrderService orderService, MealService mealService) {
        this.staffService = staffService;
        this.orderService = orderService;
        this.mealService = mealService;
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
     * Updates the status of an order (PAID -> PREPARING -> READY -> COMPLETED)
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

        Order updated = orderService.updateOrderStatusByStaff(orderId, newStatus);
        return ResponseEntity.ok(updated);
    }

    // ===== Menu management =====

    /**
     * GET /api/staff/meals
     * Full menu, including meals currently marked unavailable - unlike the
     * public /api/meals endpoint that customers see.
     */
    @GetMapping("/meals")
    public ResponseEntity<List<Meal>> getAllMeals() {
        return ResponseEntity.ok(mealService.getAllMeals());
    }

    /**
     * POST /api/staff/meals
     * Adds a new item to the menu.
     */
    @PostMapping("/meals")
    public ResponseEntity<Meal> createMeal(@RequestBody MealRequest request) {
        Meal created = mealService.createMeal(request);
        log.info("Staff created meal '{}' (id {})", created.getName(), created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/staff/meals/{id}
     * Edits an existing meal - name, description, price, category, prep
     * time, image, or availability. Only fields present in the request body
     * are changed.
     */
    @PutMapping("/meals/{id}")
    public ResponseEntity<Meal> updateMeal(@PathVariable Long id, @RequestBody MealRequest request) {
        Meal updated = mealService.updateMeal(id, request);
        log.info("Staff updated meal {} ('{}')", id, updated.getName());
        return ResponseEntity.ok(updated);
    }
}
