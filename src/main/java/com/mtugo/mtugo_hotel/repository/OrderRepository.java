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

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses AND o.id != :excludeId")
    long countByStatusInAndIdNot(@Param("statuses") List<OrderStatus> statuses, @Param("excludeId") Long excludeId);

    // Find all orders with status PAID, PREPARING, or READY, sorted by order time
    List<Order> findByStatusInOrderByOrderTimeAsc(List<OrderStatus> statuses);

    // Find all orders with status PAID (new orders waiting to be cooked)
    List<Order> findByStatusOrderByOrderTimeAsc(OrderStatus status);

    // Find all orders with status PREPARING (currently being cooked)
    List<Order> findByStatusOrderByPaidAtAsc(OrderStatus status);

    // Find all orders with status READY (awaiting pickup)
    List<Order> findByStatusOrderByExpectedReadyAtAsc(OrderStatus status);
}