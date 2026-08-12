package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.MealResponse;
import com.mtugo.mtugo_hotel.service.MealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meals")
public class MealController {

    private final MealService mealService;

    @Autowired
    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    @GetMapping
    public ResponseEntity<List<MealResponse>> getAllAvailableMeals() {
        List<MealResponse> meals = mealService.getAllAvailableMeals();
        return ResponseEntity.ok(meals);
    }
}