package com.suprith.ecommerce.repository;

public interface ProductSalesProjection {

    Long getProductId();

    String getProductName();

    Long getTotalSold();
}
