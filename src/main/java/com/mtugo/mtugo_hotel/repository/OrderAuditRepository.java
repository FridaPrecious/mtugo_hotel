package com.mtugo.mtugo_hotel.repository;

import com.mtugo.mtugo_hotel.entity.OrderAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderAuditRepository extends JpaRepository<OrderAudit, Long> {

    List<OrderAudit> findByOrderIdOrderByChangedAtAsc(Long orderId);
}
