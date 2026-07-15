package com.suprith.ecommerce.service;

import java.util.List;

import com.suprith.ecommerce.dto.AddressRequest;
import com.suprith.ecommerce.dto.AddressResponse;

public interface AddressService {

    AddressResponse create(Long userId, AddressRequest request);

    AddressResponse update(Long userId, Long addressId, AddressRequest request);

    void delete(Long userId, Long addressId);

    AddressResponse getById(Long userId, Long addressId);

    List<AddressResponse> getAll(Long userId);

    AddressResponse setDefault(Long userId, Long addressId);
}
