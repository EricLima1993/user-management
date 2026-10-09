package br.com.eric.usermanagement.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.viacep")
public record ViaCepProperties(@NotBlank String baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {}
