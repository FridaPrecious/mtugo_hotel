package com.mtugo.mtugo_hotel.service;

import com.mtugo.mtugo_hotel.dto.StaffDashboardResponse;
import com.mtugo.mtugo_hotel.dto.StaffOrderDTO;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.OrderStatus;
import com.mtugo.mtugo_hotel.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);

    private final OrderRepository orderRepository;

    @Autowired
    public StaffService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public StaffDashboardResponse getActiveOrders() {
        log.info("Fetching active orders for staff dashboard");

        // Fetch orders by status
        List<Order> paidOrders = orderRepository.findByStatusOrderByOrderTimeAsc(OrderStatus.PAID);
        List<Order> preparingOrders = orderRepository.findByStatusOrderByPaidAtAsc(OrderStatus.PREPARING);
        List<Order> readyOrders = orderRepository.findByStatusOrderByExpectedReadyAtAsc(OrderStatus.READY);

        // Map to DTOs
        List<StaffOrderDTO> paid = paidOrders.stream()
                .map(this::mapToStaffOrderDTO)
                .collect(Collectors.toList());

        List<StaffOrderDTO> preparing = preparingOrders.stream()
                .map(this::mapToStaffOrderDTO)
                .collect(Collectors.toList());

        List<StaffOrderDTO> ready = readyOrders.stream()
                .map(this::mapToStaffOrderDTO)
                .collect(Collectors.toList());

        return StaffDashboardResponse.builder()
                .paid(paid)
                .preparing(preparing)
                .ready(ready)
                .paidCount(paid.size())
                .preparingCount(preparing.size())
                .readyCount(ready.size())
                .build();
    }

    private StaffOrderDTO mapToStaffOrderDTO(Order order) {
        LocalDateTime now = LocalDateTime.now();
        long elapsedMinutes = 0;
        long waitTimeMinutes = 0;

        if (order.getOrderTime() != null) {
            elapsedMinutes = Duration.between(order.getOrderTime(), now).toMinutes();
        }

        if (order.getExpectedReadyAt() != null) {
            if (now.isBefore(order.getExpectedReadyAt())) {
                waitTimeMinutes = Duration.between(now, order.getExpectedReadyAt()).toMinutes();
            }
        }

        return StaffOrderDTO.builder()
                .id(order.getId())
                .mealName(order.getMeal().getName())
                .mealCategory(order.getMeal().getCategory())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .customerPhone(order.getCustomerPhone())
                .status(order.getStatus().name())
                .orderTime(order.getOrderTime())
                .paidAt(order.getPaidAt())
                .expectedReadyAt(order.getExpectedReadyAt())
                .requestedPickupTime(order.getRequestedPickupTime())
                .elapsedMinutes(elapsedMinutes)
                .waitTimeMinutes(waitTimeMinutes)
                .build();
    }
}