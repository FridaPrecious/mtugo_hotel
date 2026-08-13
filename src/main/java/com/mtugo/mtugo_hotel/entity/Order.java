package com.mtugo.mtugo_hotel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    // ===== Primary Key =====
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Meal Reference =====
    @ManyToOne
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    // ===== Order Details =====
    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "customer_phone", nullable = false, length = 15)
    private String customerPhone;

    // ===== Order Status =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // ===== Timestamps =====
    @Column(name = "order_time")
    private LocalDateTime orderTime;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expected_ready_at")
    private LocalDateTime expectedReadyAt;

    @Column(name = "requested_pickup_time")
    private LocalDateTime requestedPickupTime;

    // Groups multiple Order rows created from a single cart checkout, so they
    // share one payment and are progressed together (one meal = one Order row).
    @Column(name = "cart_group_id", length = 36)
    private String cartGroupId;

    // ===== M-Pesa Integration =====
    @Column(name = "checkout_request_id", length = 100)
    private String checkoutRequestId;

    // ===== Audit Fields =====
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== Lifecycle Callbacks =====
    @PrePersist
    protected void onCreate() {
        this.orderTime = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.totalAmount == null && this.meal != null) {
            this.totalAmount = this.meal.getPrice() * this.quantity;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}