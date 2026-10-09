package br.com.eric.usermanagement.dto;

import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;

public record UserFilter(String search, String city, UserStatus status, Role role) {}
