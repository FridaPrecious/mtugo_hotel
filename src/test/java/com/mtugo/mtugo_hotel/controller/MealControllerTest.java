package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.MealResponse;
import com.mtugo.mtugo_hotel.service.MealService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MealControllerTest {

    @Mock
    private MealService mealService;

    @InjectMocks
    private MealController mealController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllAvailableMeals_ShouldReturnListOfMeals() {
        // Arrange
        List<MealResponse> expectedMeals = Arrays.asList(
                MealResponse.builder()
                        .id(1L)
                        .name("Pizza Margherita")
                        .description("Classic pizza with tomato, mozzarella, and fresh basil")
                        .price(1.00)
                        .imageUrl("pizza.png")
                        .category("Pizza")
                        .build(),
                MealResponse.builder()
                        .id(2L)
                        .name("Beef Burger")
                        .description("Juicy beef patty with lettuce, tomato, and cheddar cheese")
                        .price(1.00)
                        .imageUrl("burger.png")
                        .category("Burgers")
                        .build()
        );

        when(mealService.getAllAvailableMeals()).thenReturn(expectedMeals);

        // Act
        ResponseEntity<List<MealResponse>> response = mealController.getAllAvailableMeals();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("Pizza Margherita", response.getBody().get(0).getName());
        assertEquals("Beef Burger", response.getBody().get(1).getName());

        verify(mealService, times(1)).getAllAvailableMeals();
    }

    @Test
    void getAllAvailableMeals_WhenNoMeals_ShouldReturnEmptyList() {
        // Arrange
        when(mealService.getAllAvailableMeals()).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<MealResponse>> response = mealController.getAllAvailableMeals();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());

        verify(mealService, times(1)).getAllAvailableMeals();
    }
}