package com.mtugo.mtugo_hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    @Builder.Default
    private List<CartItemDTO> items = new ArrayList<>();
    private Integer totalItems;
    private Double totalAmount;

    public void calculateTotals() {
        this.totalItems = items.stream().mapToInt(CartItemDTO::getQuantity).sum();
        this.totalAmount = items.stream().mapToDouble(CartItemDTO::getSubtotal).sum();
    }
}