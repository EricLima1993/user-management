package br.com.eric.usermanagement.service;

import br.com.eric.usermanagement.client.CepClient;
import br.com.eric.usermanagement.client.CepData;
import br.com.eric.usermanagement.domain.entity.Address;
import br.com.eric.usermanagement.exception.CepServiceUnavailableException;
import br.com.eric.usermanagement.exception.InvalidCepException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CepValidationServiceTest {

    @Mock
    private CepClient cepClient;

    @InjectMocks
    private CepValidationService service;

    private Address typedAddress() {
        Address address = new Address();
        address.setCep("01001000");
        address.setStreet("Rua Digitada");
        address.setDistrict("Bairro Digitado");
        address.setCity("Cidade Errada");
        address.setState("RJ");
        return address;
    }

    @Test
    void overwritesStateCityStreetAndDistrictWithViaCepData() {
        when(cepClient.findByCep("01001000"))
                .thenReturn(Optional.of(new CepData("01001000", "Praça da Sé", "Sé", "São Paulo", "SP")));
        Address address = typedAddress();

        service.validateAndEnrich(address);

        assertThat(address.getState()).isEqualTo("SP");
        assertThat(address.getCity()).isEqualTo("São Paulo");
        assertThat(address.getStreet()).isEqualTo("Praça da Sé");
        assertThat(address.getDistrict()).isEqualTo("Sé");
    }

    @Test
    void keepsTypedStreetAndDistrictWhenViaCepDoesNotProvideThem() {
        when(cepClient.findByCep("01001000"))
                .thenReturn(Optional.of(new CepData("01001000", "", null, "Cidade Pequena", "MG")));
        Address address = typedAddress();

        service.validateAndEnrich(address);

        assertThat(address.getCity()).isEqualTo("Cidade Pequena");
        assertThat(address.getState()).isEqualTo("MG");
        assertThat(address.getStreet()).isEqualTo("Rua Digitada");
        assertThat(address.getDistrict()).isEqualTo("Bairro Digitado");
    }

    @Test
    void throwsInvalidCepWhenCepDoesNotExist() {
        when(cepClient.findByCep("01001000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndEnrich(typedAddress()))
                .isInstanceOf(InvalidCepException.class)
                .hasMessageContaining("01001000");
    }

    @Test
    void propagatesServiceUnavailable() {
        when(cepClient.findByCep("01001000"))
                .thenThrow(new CepServiceUnavailableException(new RuntimeException("fora do ar")));

        assertThatThrownBy(() -> service.validateAndEnrich(typedAddress()))
                .isInstanceOf(CepServiceUnavailableException.class);
    }
}
