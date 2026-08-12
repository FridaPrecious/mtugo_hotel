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