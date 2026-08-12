package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.MpesaStkPushRequest;
import com.mtugo.mtugo_hotel.dto.MpesaStkPushResponse;
import com.mtugo.mtugo_hotel.service.MpesaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mpesa")
public class MpesaController {

    private final MpesaService mpesaService;

    @Autowired
    public MpesaController(MpesaService mpesaService) {
        this.mpesaService = mpesaService;
    }

    @PostMapping("/stkpush")
    public ResponseEntity<MpesaStkPushResponse> initiateStkPush(@RequestBody MpesaStkPushRequest request) {
        MpesaStkPushResponse response = mpesaService.initiateStkPush(request);
        return ResponseEntity.ok(response);
    }
}