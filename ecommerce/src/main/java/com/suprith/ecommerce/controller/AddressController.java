package com.suprith.ecommerce.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.suprith.ecommerce.dto.AddressRequest;
import com.suprith.ecommerce.dto.AddressResponse;
import com.suprith.ecommerce.security.UserPrincipal;
import com.suprith.ecommerce.service.AddressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(addressService.getAll(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getById(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long id) {
        return ResponseEntity.ok(addressService.getById(principal.getId(), id));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        addressService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(addressService.setDefault(principal.getId(), id));
    }
}
