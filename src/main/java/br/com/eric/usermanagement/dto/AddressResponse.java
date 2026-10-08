package br.com.eric.usermanagement.dto;

public record AddressResponse(Long id, String cep, String street, String number, String complement,
                              String state, String city, String district, boolean mainAddress) {}
