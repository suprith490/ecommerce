package com.suprith.ecommerce.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Addresses can be edited or deleted after an order ships, so the order stores
 * its own frozen copy of the delivery details rather than a live FK to Address.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddressSnapshot {

    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
