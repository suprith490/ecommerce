package com.suprith.ecommerce.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.suprith.ecommerce.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select oi.product.id as productId, oi.productName as productName, sum(oi.quantity) as totalSold "
            + "from OrderItem oi "
            + "where oi.product is not null "
            + "group by oi.product.id, oi.productName "
            + "order by sum(oi.quantity) desc")
    List<ProductSalesProjection> findTopSellingProducts(Pageable pageable);
}
