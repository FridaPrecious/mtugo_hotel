package com.mtugo.mtugo_hotel.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull(message = "Meal ID is required")
    private Long mealId;

    @Builder.Default
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    @NotNull(message = "Phone number is required")
    @Pattern(regexp = "^(254[0-9]{9}|07[0-9]{8})$", message = "Phone must be in format 2547XXXXXXXX or 07XXXXXXXX")
    private String phone;

    /**
     * Optional. Customer's preferred pickup time. If omitted, the kitchen's
     * standard prep-time estimate is used instead.
     */
    private LocalDateTime pickupTime;
}