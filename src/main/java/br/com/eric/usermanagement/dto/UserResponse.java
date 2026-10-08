package br.com.eric.usermanagement.dto;

import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(Long id, String name, String email, String phone, Role role, UserStatus status,
                           LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy, String updatedBy,
                           List<AddressResponse> addresses) {}
