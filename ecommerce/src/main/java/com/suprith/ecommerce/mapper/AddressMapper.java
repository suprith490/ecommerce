package com.suprith.ecommerce.mapper;

import org.springframework.stereotype.Component;

import com.suprith.ecommerce.dto.AddressResponse;
import com.suprith.ecommerce.entity.Address;

@Component
public class AddressMapper {

    public AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .type(address.getType())
                .isDefault(address.isDefault())
                .build();
    }
}
