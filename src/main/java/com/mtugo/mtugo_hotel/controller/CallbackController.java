package com.mtugo.mtugo_hotel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtugo.mtugo_hotel.entity.Transaction;
import com.mtugo.mtugo_hotel.repository.TransactionRepository;
import com.mtugo.mtugo_hotel.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mpesa")
public class CallbackController {

    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);

    private final TransactionRepository transactionRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public CallbackController(TransactionRepository transactionRepository, OrderService orderService) {
        this.transactionRepository = transactionRepository;
        this.orderService = orderService;
    }

    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestBody String payload) {
        log.info("Received M-Pesa callback: {}", payload);

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode stkCallback = root.path("Body").path("stkCallback");

            String checkoutRequestId = stkCallback.path("CheckoutRequestID").asText();
            String merchantRequestId = stkCallback.path("MerchantRequestID").asText();
            int resultCode = stkCallback.path("ResultCode").asInt();
            String resultDesc = stkCallback.path("ResultDesc").asText();

            log.info("Callback: CheckoutRequestID: {}, ResultCode: {}, ResultDesc: {}",
                    checkoutRequestId, resultCode, resultDesc);

            // Find the transaction by CheckoutRequestID
            Transaction transaction = transactionRepository.findByCheckoutRequestId(checkoutRequestId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found for checkoutRequestId: " + checkoutRequestId));

            if (resultCode == 0) {
                // Payment successful
                JsonNode metadata = stkCallback.path("CallbackMetadata");
                String receiptNumber = "";
                if (metadata.has("Item")) {
                    for (JsonNode item : metadata.path("Item")) {
                        if ("MpesaReceiptNumber".equals(item.path("Name").asText())) {
                            receiptNumber = item.path("Value").asText();
                            break;
                        }
                    }
                }
                orderService.completeTransactionAndOrder(transaction.getId(), receiptNumber, resultDesc);
                log.info("Transaction {} and order {} marked as PAID", transaction.getId(), transaction.getOrder().getId());
            } else {
                // Payment failed
                orderService.failTransactionAndOrder(transaction.getId(), resultDesc);
                log.info("Transaction {} and order {} marked as FAILED", transaction.getId(), transaction.getOrder().getId());
            }

            // Always return 200 to acknowledge receipt
            return ResponseEntity.ok("Received");

        } catch (Exception e) {
            log.error("Error processing callback", e);
            return ResponseEntity.status(500).body("Error processing callback: " + e.getMessage());
        }
    }
}