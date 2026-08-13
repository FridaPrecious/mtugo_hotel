package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReceiptController {

    private final ReceiptService receiptService;

    @Autowired
    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    /**
     * GET /receipt/{orderId} - downloads the receipt as a .txt file
     */
    @GetMapping("/receipt/{orderId}")
    public ResponseEntity<String> downloadReceipt(@PathVariable Long orderId) {
        String receipt = receiptService.generateReceiptText(orderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"mtugo-receipt-order-" + orderId + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(receipt);
    }
}