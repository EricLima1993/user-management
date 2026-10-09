package br.com.eric.usermanagement.client;

import java.util.Optional;

public interface CepClient {
    Optional<CepData> findByCep(String cep);
}
