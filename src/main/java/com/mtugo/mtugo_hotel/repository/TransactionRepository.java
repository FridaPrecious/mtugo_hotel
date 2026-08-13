package com.mtugo.mtugo_hotel.repository;

import com.mtugo.mtugo_hotel.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByCheckoutRequestId(String checkoutRequestId);
    Optional<Transaction> findTopByOrder_IdOrderByIdDesc(Long orderId);
}