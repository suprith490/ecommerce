package com.suprith.ecommerce.service;

import java.time.LocalDate;

import com.suprith.ecommerce.dto.DashboardStatsResponse;
import com.suprith.ecommerce.dto.SalesReportResponse;

public interface AnalyticsService {

    DashboardStatsResponse getDashboardStats();

    SalesReportResponse getSalesReport(LocalDate fromDate, LocalDate toDate);
}
