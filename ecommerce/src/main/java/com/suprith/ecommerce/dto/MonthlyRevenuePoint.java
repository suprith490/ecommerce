package com.suprith.ecommerce.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyRevenuePoint {

    /** Format: yyyy-MM, e.g. "2026-07" */
    private String month;
    private BigDecimal revenue;
    private long orderCount;
}
