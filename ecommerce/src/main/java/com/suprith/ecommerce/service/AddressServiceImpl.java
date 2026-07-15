package com.suprith.ecommerce.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.AddressRequest;
import com.suprith.ecommerce.dto.AddressResponse;
import com.suprith.ecommerce.entity.Address;
import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.enums.AddressType;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.mapper.AddressMapper;
import com.suprith.ecommerce.repository.AddressRepository;
import com.suprith.ecommerce.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    public AddressResponse create(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isFirstAddress = addressRepository.countByUserId(userId) == 0;
        boolean shouldBeDefault = isFirstAddress || request.isDefault();

        if (shouldBeDefault) {
            clearExistingDefault(userId);
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry() : "India")
                .type(request.getType() != null ? request.getType() : AddressType.HOME)
                .isDefault(shouldBeDefault)
                .build();

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        Address address = findOwned(userId, addressId);

        if (request.isDefault() && !address.isDefault()) {
            clearExistingDefault(userId);
            address.setDefault(true);
        }

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            address.setCountry(request.getCountry());
        }
        if (request.getType() != null) {
            address.setType(request.getType());
        }

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    public void delete(Long userId, Long addressId) {
        Address address = findOwned(userId, addressId);
        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        addressRepository.save(next);
                    });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getById(Long userId, Long addressId) {
        return addressMapper.toResponse(findOwned(userId, addressId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAll(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    public AddressResponse setDefault(Long userId, Long addressId) {
        Address address = findOwned(userId, addressId);
        clearExistingDefault(userId);
        address.setDefault(true);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    private void clearExistingDefault(Long userId) {
        addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                .filter(Address::isDefault)
                .forEach(a -> {
                    a.setDefault(false);
                    addressRepository.save(a);
                });
    }

    private Address findOwned(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
