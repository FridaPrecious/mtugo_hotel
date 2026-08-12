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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "customer_phone", nullable = false, length = 15)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "order_time")
    private LocalDateTime orderTime;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expected_ready_at")
    private LocalDateTime expectedReadyAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "checkout_request_id", length = 100)
    private String checkoutRequestId;

    @PrePersist
    protected void onCreate() {
        orderTime = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalAmount == null && meal != null) {
            totalAmount = meal.getPrice() * quantity;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}