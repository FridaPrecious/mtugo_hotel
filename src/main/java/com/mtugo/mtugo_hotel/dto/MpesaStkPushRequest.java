package com.mtugo.mtugo_hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaStkPushRequest {
    private Long orderId;
    private String phone;
    private Double amount;
}