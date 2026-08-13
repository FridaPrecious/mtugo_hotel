package com.mtugo.mtugo_hotel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpesaStkPushResponse {
    @JsonProperty("MerchantRequestID")
    private String MerchantRequestID;

    @JsonProperty("CheckoutRequestID")
    private String CheckoutRequestID;

    @JsonProperty("ResponseCode")
    private String ResponseCode;

    @JsonProperty("ResponseDescription")
    private String ResponseDescription;

    @JsonProperty("CustomerMessage")
    private String CustomerMessage;
}