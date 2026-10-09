package br.com.eric.usermanagement.dto;

import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;

public record UserSummaryResponse(Long id, String name, String email, String phone,
                                  Role role, UserStatus status, String city, String state) {}
