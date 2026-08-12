package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.MpesaStkPushRequest;
import com.mtugo.mtugo_hotel.dto.MpesaStkPushResponse;
import com.mtugo.mtugo_hotel.entity.Transaction;
import com.mtugo.mtugo_hotel.service.MpesaService;
import com.mtugo.mtugo_hotel.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mpesa")
public class MpesaController {

    private static final Logger log = LoggerFactory.getLogger(MpesaController.class);

    private final MpesaService mpesaService;
    private final OrderService orderService;

    @Autowired
    public MpesaController(MpesaService mpesaService, OrderService orderService) {
        this.mpesaService = mpesaService;
        this.orderService = orderService;
    }

    @PostMapping("/stkpush")
    public ResponseEntity<MpesaStkPushResponse> initiateStkPush(@RequestBody MpesaStkPushRequest request) {
        log.info("STK Push request received - orderId: {}, phone: {}, amount: {}",
                request.getOrderId(), request.getPhone(), request.getAmount());

        try {
            Transaction transaction = orderService.createTransaction(
                    request.getOrderId(),
                    request.getPhone(),
                    request.getAmount()
            );
            log.info("Transaction created with id: {}", transaction.getId());

            MpesaStkPushResponse response = mpesaService.initiateStkPush(request);

            if ("0".equals(response.getResponseCode())) {
                orderService.updateTransactionCheckoutRequestId(
                        transaction.getId(),
                        response.getCheckoutRequestID()
                );
                log.info("Transaction {} updated with CheckoutRequestID: {}",
                        transaction.getId(), response.getCheckoutRequestID());

                orderService.updateOrderCheckoutRequestId(
                        request.getOrderId(),
                        response.getCheckoutRequestID()
                );
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to initiate STK Push", e);
            throw new RuntimeException("Failed to initiate payment: " + e.getMessage(), e);
        }
    }

    // ===== TEST ENDPOINT TO VERIFY CREDENTIALS =====
    @GetMapping("/test/token")
    public ResponseEntity<Map<String, String>> testToken() {
        Map<String, String> response = new HashMap<>();
        try {
            String token = mpesaService.getAccessToken();
            response.put("status", "success");
            response.put("message", "Access token obtained successfully");
            response.put("token", token.substring(0, 20) + "...");
            response.put("token_length", String.valueOf(token.length()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token test failed", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}