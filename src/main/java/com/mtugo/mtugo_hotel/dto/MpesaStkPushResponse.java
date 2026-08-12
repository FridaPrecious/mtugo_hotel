package com.mtugo.mtugo_hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaStkPushResponse {
    private String MerchantRequestID;
    private String CheckoutRequestID;
    private String ResponseCode;
    private String ResponseDescription;
}