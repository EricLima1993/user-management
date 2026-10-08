package br.com.eric.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        Long id,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 dígitos") String cep,
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "Estado deve ser a UF com 2 letras maiúsculas") String state,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String district,
        boolean mainAddress) {}
