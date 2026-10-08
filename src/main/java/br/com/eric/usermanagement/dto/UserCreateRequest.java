package br.com.eric.usermanagement.dto;

import br.com.eric.usermanagement.domain.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record UserCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        @NotEmpty @Valid List<AddressRequest> addresses) {}
