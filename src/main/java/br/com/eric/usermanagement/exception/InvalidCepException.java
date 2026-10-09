package br.com.eric.usermanagement.exception;

public class InvalidCepException extends BusinessRuleException {
    public InvalidCepException(String cep) {
        super("CEP não encontrado: " + cep);
    }
}
