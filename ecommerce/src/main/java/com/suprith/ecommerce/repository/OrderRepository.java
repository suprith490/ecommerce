package com.suprith.ecommerce.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.suprith.ecommerce.entity.Order;
import com.suprith.ecommerce.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByPlacedAtDesc(Long userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Page<Order> findByStatusOrderByPlacedAtDesc(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByUserId(Long userId);

    List<Order> findByStatusNot(OrderStatus excludedStatus);

    List<Order> findByPlacedAtBetweenAndStatusNot(LocalDateTime from, LocalDateTime to, OrderStatus excludedStatus);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.status <> :excludedStatus")
    BigDecimal sumRevenue(@Param("excludedStatus") OrderStatus excludedStatus);
}
