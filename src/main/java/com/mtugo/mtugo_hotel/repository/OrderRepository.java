package com.mtugo.mtugo_hotel.repository;

import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Other methods if any...

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses AND o.id != :excludeId")
    long countByStatusInAndIdNot(@Param("statuses") List<OrderStatus> statuses, @Param("excludeId") Long excludeId);
}