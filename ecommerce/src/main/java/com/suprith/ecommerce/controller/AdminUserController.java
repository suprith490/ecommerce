package com.suprith.ecommerce.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.suprith.ecommerce.dto.AdminUserResponse;
import com.suprith.ecommerce.security.UserPrincipal;
import com.suprith.ecommerce.service.UserManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(userManagementService.getAllUsers(search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userManagementService.getById(id));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<AdminUserResponse> disableUser(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userManagementService.disableUser(id, principal.getId()));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<AdminUserResponse> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(userManagementService.enableUser(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        userManagementService.deleteUser(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
