package com.suprith.ecommerce.dto;

import com.suprith.ecommerce.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotNull(message = "Delivery address is required")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /** Optional — must have already been validated via the cart coupon preview endpoint. */
    private String couponCode;
}
