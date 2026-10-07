package br.com.eric.usermanagement.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
