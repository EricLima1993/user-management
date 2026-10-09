package br.com.eric.usermanagement.exception;

public class CepServiceUnavailableException extends RuntimeException {
    public CepServiceUnavailableException(Throwable cause) {
        super("Serviço de consulta de CEP indisponível. Tente novamente em instantes.", cause);
    }
}
