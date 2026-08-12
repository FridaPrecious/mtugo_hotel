package com.mtugo.mtugo_hotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtugo.mtugo_hotel.config.MpesaConfig;
import com.mtugo.mtugo_hotel.dto.MpesaStkPushRequest;
import com.mtugo.mtugo_hotel.dto.MpesaStkPushResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class MpesaService {

    private static final Logger log = LoggerFactory.getLogger(MpesaService.class);

    private final MpesaConfig mpesaConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache token with expiry
    private String cachedToken;
    private long tokenExpiryTime;

    @Autowired
    public MpesaService(MpesaConfig mpesaConfig) {
        this.mpesaConfig = mpesaConfig;
    }

    /**
     * Get OAuth access token from Daraja API
     */
    public String getAccessToken() throws Exception {
        // Check if cached token is still valid (expires in 3599 seconds)
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }

        log.info("Fetching new access token from Daraja...");

        String credentials = mpesaConfig.getConsumerKey() + ":" + mpesaConfig.getConsumerSecret();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mpesaConfig.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials"))
                .header("Authorization", "Basic " + encodedCredentials)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Failed to get access token. Status: {}, Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("Failed to get access token: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String token = json.get("access_token").asText();
        int expiresIn = json.get("expires_in").asInt(); // usually 3599

        // Cache token with 60-second buffer
        cachedToken = token;
        tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;

        log.info("Access token obtained successfully. Expires in {} seconds.", expiresIn);
        return token;
    }

    /**
     * Initiate STK Push payment
     */
    public MpesaStkPushResponse initiateStkPush(MpesaStkPushRequest request) {
        try {
            log.info("Initiating STK Push - orderId: {}, phone: {}, amount: {}",
                    request.getOrderId(), request.getPhone(), request.getAmount());

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            // Generate password: Base64(Shortcode + Passkey + Timestamp)
            String passwordStr = mpesaConfig.getShortcode() + mpesaConfig.getPasskey() + timestamp;
            String password = Base64.getEncoder().encodeToString(passwordStr.getBytes());

            // Format phone number: remove any spaces, ensure 254 prefix
            String phone = formatPhoneNumber(request.getPhone());

            // Build request body
            String requestBody = String.format(
                    "{\"BusinessShortCode\":\"%s\",\"Password\":\"%s\",\"Timestamp\":\"%s\"," +
                    "\"TransactionType\":\"CustomerPayBillOnline\",\"Amount\":\"%d\"," +
                    "\"PartyA\":\"%s\",\"PartyB\":\"%s\",\"PhoneNumber\":\"%s\"," +
                    "\"CallBackURL\":\"%s\",\"AccountReference\":\"ORDER-%d\"," +
                    "\"TransactionDesc\":\"Mtugo Hotel Meal Payment\"}",
                    mpesaConfig.getShortcode(),
                    password,
                    timestamp,
                    request.getAmount().intValue(),
                    phone,
                    mpesaConfig.getShortcode(),
                    phone,
                    mpesaConfig.getCallbackUrl(),
                    request.getOrderId()
            );

            log.debug("STK Push request: {}", requestBody);

            String accessToken = getAccessToken();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(mpesaConfig.getBaseUrl() + "/mpesa/stkpush/v1/processrequest"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            log.info("STK Push response: {}", response.body());

            if (response.statusCode() != 200) {
                log.error("STK Push failed. Status: {}, Body: {}", response.statusCode(), response.body());
                throw new RuntimeException("STK Push failed: " + response.body());
            }

            return objectMapper.readValue(response.body(), MpesaStkPushResponse.class);

        } catch (Exception e) {
            log.error("Error initiating STK Push", e);
            throw new RuntimeException("Failed to initiate STK Push: " + e.getMessage(), e);
        }
    }

    /**
     * Format phone number to international format (2547XXXXXXXX)
     */
    private String formatPhoneNumber(String phone) {
        // Remove all non-digit characters
        String cleaned = phone.replaceAll("\\D", "");

        // If starts with 0, replace with 254
        if (cleaned.startsWith("0")) {
            cleaned = "254" + cleaned.substring(1);
        }
        // If starts with 7, add 254
        else if (cleaned.startsWith("7")) {
            cleaned = "254" + cleaned;
        }
        // If starts with 254, keep as is
        else if (!cleaned.startsWith("254")) {
            // If it's a short number, assume it's missing 254
            cleaned = "254" + cleaned;
        }

        log.debug("Formatted phone: {} -> {}", phone, cleaned);
        return cleaned;
    }
}