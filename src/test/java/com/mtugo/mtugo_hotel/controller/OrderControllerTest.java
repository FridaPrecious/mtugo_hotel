package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.OrderRequest;
import com.mtugo.mtugo_hotel.dto.OrderResponse;
import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.OrderStatus;
import com.mtugo.mtugo_hotel.repository.MealRepository;
import com.mtugo.mtugo_hotel.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MealRepository mealRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Meal testMeal;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        mealRepository.deleteAll();

        testMeal = mealRepository.save(new Meal(null, "Test Pizza",
                "Test description", 1.00, "/images/pizza.jpg",
                "Pizza", 15, true));
    }

    @Test
    void createOrder_ShouldReturnCreatedOrder() {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .mealId(testMeal.getId())
                .quantity(2)
                .phone("254712345678")
                .build();

        String url = "http://localhost:" + port + "/api/orders";

        // Act
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(url, request, OrderResponse.class);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getOrderId());
        assertEquals(2.00, response.getBody().getTotalAmount());
        assertEquals(OrderStatus.PENDING.name(), response.getBody().getStatus());

        // Verify order was saved in database
        Order savedOrder = orderRepository.findById(response.getBody().getOrderId()).orElse(null);
        assertNotNull(savedOrder);
        assertEquals(testMeal.getId(), savedOrder.getMeal().getId());
        assertEquals(2, savedOrder.getQuantity());
        assertEquals(2.00, savedOrder.getTotalAmount());
        assertEquals("254712345678", savedOrder.getCustomerPhone());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
    }

    @Test
    void createOrder_WithInvalidMealId_ShouldReturn404() {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .mealId(999L)
                .quantity(1)
                .phone("254712345678")
                .build();

        String url = "http://localhost:" + port + "/api/orders";

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    void createOrder_WithZeroQuantity_ShouldReturn400() {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .mealId(testMeal.getId())
                .quantity(0)
                .phone("254712345678")
                .build();

        String url = "http://localhost:" + port + "/api/orders";

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("quantity"));
    }

    @GetMapping("/test")
        public ResponseEntity<String> test() {
        return ResponseEntity.ok("Order controller is working");
    }

    @Test
    void createOrder_WithInvalidPhone_ShouldReturn400() {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .mealId(testMeal.getId())
                .quantity(1)
                .phone("0712345678") // Invalid format (should be 2547XXXXXXXX)
                .build();

        String url = "http://localhost:" + port + "/api/orders";

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("phone"));
    }
}