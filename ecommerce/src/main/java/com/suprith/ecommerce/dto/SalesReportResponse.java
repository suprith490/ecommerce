package com.suprith.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalesReportResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal totalRevenue;
    private long totalOrders;
    private BigDecimal averageOrderValue;
    private List<TopSellingProductResponse> topSellingProducts;
}
