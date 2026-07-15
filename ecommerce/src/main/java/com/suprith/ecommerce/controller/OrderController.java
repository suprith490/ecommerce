package com.suprith.ecommerce.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.suprith.ecommerce.dto.CheckoutRequest;
import com.suprith.ecommerce.dto.OrderActionRequest;
import com.suprith.ecommerce.dto.OrderResponse;
import com.suprith.ecommerce.dto.OrderStatusUpdateRequest;
import com.suprith.ecommerce.dto.OrderSummaryResponse;
import com.suprith.ecommerce.security.UserPrincipal;
import com.suprith.ecommerce.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ---- Customer ----

    @PostMapping("/api/orders/checkout")
    public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.placeOrder(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/orders")
    public ResponseEntity<Page<OrderSummaryResponse>> getOrderHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("placedAt").descending());
        return ResponseEntity.ok(orderService.getOrderHistory(principal.getId(), pageable));
    }

    @GetMapping("/api/orders/{id}")
    public ResponseEntity<OrderResponse> getOrderDetail(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(principal.getId(), id));
    }

    @PostMapping("/api/orders/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody OrderActionRequest request) {
        return ResponseEntity.ok(orderService.cancelOrder(principal.getId(), id, request.getReason()));
    }

    @PostMapping("/api/orders/{id}/return")
    public ResponseEntity<OrderResponse> requestReturn(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody OrderActionRequest request) {
        return ResponseEntity.ok(orderService.requestReturn(principal.getId(), id, request.getReason()));
    }

    // ---- Admin ----

    @GetMapping("/api/admin/orders")
    public ResponseEntity<Page<OrderResponse>> getAllForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("placedAt").descending());
        return ResponseEntity.ok(orderService.getAllForAdmin(pageable));
    }

    @GetMapping("/api/admin/orders/{id}")
    public ResponseEntity<OrderResponse> getByIdForAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getByIdForAdmin(id));
    }

    @PatchMapping("/api/admin/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }
}
