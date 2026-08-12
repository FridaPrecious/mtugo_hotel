package com.mtugo.mtugo_hotel.service;

import com.mtugo.mtugo_hotel.dto.OrderRequest;
import com.mtugo.mtugo_hotel.dto.OrderResponse;
import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.OrderStatus;
import com.mtugo.mtugo_hotel.repository.MealRepository;
import com.mtugo.mtugo_hotel.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MealRepository mealRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, MealRepository mealRepository) {
        this.orderRepository = orderRepository;
        this.mealRepository = mealRepository;
    }

    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order - mealId: {}, quantity: {}, phone: {}",
                request.getMealId(), request.getQuantity(), request.getPhone());

        // Find the meal
        Meal meal = mealRepository.findById(request.getMealId())
                .orElseThrow(() -> {
                    log.error("Meal not found with id: {}", request.getMealId());
                    return new RuntimeException("Meal not found with id: " + request.getMealId());
                });

        log.info("Found meal: {} (price: {})", meal.getName(), meal.getPrice());

        // Calculate total amount
        Double totalAmount = meal.getPrice() * request.getQuantity();
        log.info("Total amount calculated: {}", totalAmount);

        // Create and save order
        Order order = new Order();
        order.setMeal(meal);
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(totalAmount);
        order.setCustomerPhone(request.getPhone());
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with id: {}", savedOrder.getId());

        return OrderResponse.builder()
                .orderId(savedOrder.getId())
                .totalAmount(savedOrder.getTotalAmount())
                .status(savedOrder.getStatus().name())
                .message("Order created successfully")
                .build();
    }
}