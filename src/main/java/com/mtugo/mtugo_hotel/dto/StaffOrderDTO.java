package com.mtugo.mtugo_hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOrderDTO {
    private Long id;
    private String mealName;
    private String mealCategory;
    private Integer quantity;
    private Double totalAmount;
    private String customerPhone;
    private String status;
    private LocalDateTime orderTime;
    private LocalDateTime paidAt;
    private LocalDateTime expectedReadyAt;
    private Long elapsedMinutes;
    private Long waitTimeMinutes;
}