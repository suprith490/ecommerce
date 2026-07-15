package com.suprith.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartResponse {

    private List<CartItemResponse> items;
    private int itemCount;

    private BigDecimal subtotal;
    private BigDecimal shippingCharge;
    private BigDecimal gst;
    private BigDecimal total;

    /** How much more to add to the cart to unlock free shipping. Zero if already qualified. */
    private BigDecimal amountToFreeShipping;
}
