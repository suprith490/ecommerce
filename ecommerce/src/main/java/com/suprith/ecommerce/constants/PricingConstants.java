package com.suprith.ecommerce.constants;

import java.math.BigDecimal;

/**
 * Central place for storefront pricing rules so Cart, Checkout, and Order
 * modules all compute totals the same way. Values are placeholders suitable
 * for a demo/portfolio build — wire these to admin-configurable settings
 * before using this in a real store.
 */
public final class PricingConstants {

    private PricingConstants() {
    }

    public static final BigDecimal FLAT_SHIPPING_CHARGE = new BigDecimal("49.00");

    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("499.00");

    /** 18% GST, applied on the discounted subtotal. */
    public static final BigDecimal GST_RATE = new BigDecimal("0.18");
}
