package com.mtugo.mtugo_hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight status payload used for polling - by the floating tracker
 * banner (order-tracker.js) and by the standalone "Track My Order" page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusResponse {
    private Long orderId;
    private String status;
    private String mealName;
    private Integer quantity;
    private LocalDateTime orderTime;
    private LocalDateTime paidAt;
    private LocalDateTime expectedReadyAt;
}
