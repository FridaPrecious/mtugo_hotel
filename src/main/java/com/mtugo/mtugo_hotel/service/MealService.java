package com.mtugo.mtugo_hotel.service;

import com.mtugo.mtugo_hotel.dto.MealResponse;
import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.repository.MealRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MealService {

    private final MealRepository mealRepository;

    @Autowired
    public MealService(MealRepository mealRepository) {
        this.mealRepository = mealRepository;
    }

    public List<MealResponse> getAllAvailableMeals() {
        List<Meal> meals = mealRepository.findByIsAvailableTrue();
        return meals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ADD THIS METHOD
    public Meal getMealById(Long id) {
        return mealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meal not found with id: " + id));
    }

    /**
     * Staff-facing listing - includes unavailable meals too, unlike the
     * public menu which only shows what customers can currently order.
     */
    public List<Meal> getAllMeals() {
        return mealRepository.findAll();
    }

    public Meal createMeal(com.mtugo.mtugo_hotel.dto.MealRequest request) {
        Meal meal = new Meal();
        applyRequest(meal, request);
        return mealRepository.save(meal);
    }

    public Meal updateMeal(Long id, com.mtugo.mtugo_hotel.dto.MealRequest request) {
        Meal meal = getMealById(id);
        applyRequest(meal, request);
        return mealRepository.save(meal);
    }

    private void applyRequest(Meal meal, com.mtugo.mtugo_hotel.dto.MealRequest request) {
        if (request.getName() != null) meal.setName(request.getName());
        if (request.getDescription() != null) meal.setDescription(request.getDescription());
        if (request.getPrice() != null) meal.setPrice(request.getPrice());
        if (request.getImageUrl() != null) meal.setImageUrl(request.getImageUrl());
        if (request.getCategory() != null) meal.setCategory(request.getCategory());
        if (request.getPrepTimeMinutes() != null) meal.setPrepTimeMinutes(request.getPrepTimeMinutes());
        if (request.getIsAvailable() != null) meal.setIsAvailable(request.getIsAvailable());
    }

    private MealResponse mapToResponse(Meal meal) {
        return MealResponse.builder()
                .id(meal.getId())
                .name(meal.getName())
                .description(meal.getDescription())
                .price(meal.getPrice())
                .imageUrl(meal.getImageUrl())
                .category(meal.getCategory())
                .build();
    }
}