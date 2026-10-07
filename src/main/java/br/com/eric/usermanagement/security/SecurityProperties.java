package br.com.eric.usermanagement.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(@Valid Jwt jwt, @Valid Admin admin) {

    public record Jwt(@NotBlank @Size(min = 32) String secret, @Positive long expirationMinutes) {}

    public record Admin(@NotBlank String name, @NotBlank String email, @NotBlank String password) {}
}
