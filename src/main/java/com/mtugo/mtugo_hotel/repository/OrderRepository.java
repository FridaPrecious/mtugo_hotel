package com.mtugo.mtugo_hotel.repository;

import com.mtugo.mtugo_hotel.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}