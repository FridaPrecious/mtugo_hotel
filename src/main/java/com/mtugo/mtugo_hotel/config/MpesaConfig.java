package com.mtugo.mtugo_hotel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MpesaConfig {

    @Value("${mpesa.environment:sandbox}")
    private String environment;

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.callback-url}")
    private String callbackUrl;

    public String getBaseUrl() {
        return "sandbox".equalsIgnoreCase(environment)
                ? "https://sandbox.safaricom.co.ke"
                : "https://api.safaricom.co.ke";
    }

    // Getters
    public String getEnvironment() { return environment; }
    public String getConsumerKey() { return consumerKey; }
    public String getConsumerSecret() { return consumerSecret; }
    public String getShortcode() { return shortcode; }
    public String getPasskey() { return passkey; }
    public String getCallbackUrl() { return callbackUrl; }
}