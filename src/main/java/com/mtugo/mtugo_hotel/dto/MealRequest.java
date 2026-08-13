package com.mtugo.mtugo_hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealRequest {
    private String name;
    private String description;
    private Double price;
    private String imageUrl;
    private String category;
    private Integer prepTimeMinutes;
    private Boolean isAvailable;
}
