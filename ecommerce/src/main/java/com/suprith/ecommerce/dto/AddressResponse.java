package com.suprith.ecommerce.dto;

import com.suprith.ecommerce.enums.AddressType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {

    private Long id;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private AddressType type;
    private boolean isDefault;
}
