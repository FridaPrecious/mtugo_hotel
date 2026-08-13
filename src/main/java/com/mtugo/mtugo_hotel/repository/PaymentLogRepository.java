package com.mtugo.mtugo_hotel.repository;

import com.mtugo.mtugo_hotel.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    List<PaymentLog> findByTransactionIdOrderByTimestampAsc(Long transactionId);
}
