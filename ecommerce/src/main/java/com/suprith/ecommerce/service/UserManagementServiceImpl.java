package com.suprith.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.AdminUserResponse;
import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.enums.Role;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.repository.OrderRepository;
import com.suprith.ecommerce.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(String search, Pageable pageable) {
        Page<User> users = (search == null || search.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable);
        return users.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public AdminUserResponse disableUser(Long targetUserId, Long requestingAdminId) {
        if (targetUserId.equals(requestingAdminId)) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }

        User user = findEntity(targetUserId);
        guardLastAdmin(user, "disabled");

        user.setEnabled(false);
        return toResponse(userRepository.save(user));
    }

    @Override
    public AdminUserResponse enableUser(Long targetUserId) {
        User user = findEntity(targetUserId);
        user.setEnabled(true);
        return toResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long targetUserId, Long requestingAdminId) {
        if (targetUserId.equals(requestingAdminId)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        User user = findEntity(targetUserId);
        guardLastAdmin(user, "deleted");

        if (orderRepository.countByUserId(targetUserId) > 0) {
            throw new IllegalStateException(
                    "Cannot delete a user with existing orders — disable the account instead to preserve order history.");
        }

        userRepository.delete(user);
    }

    private void guardLastAdmin(User user, String action) {
        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalStateException("Cannot " + action + " the only remaining admin account");
        }
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
