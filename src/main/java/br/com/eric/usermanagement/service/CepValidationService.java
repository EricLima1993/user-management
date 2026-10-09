package br.com.eric.usermanagement.service;

import br.com.eric.usermanagement.client.CepClient;
import br.com.eric.usermanagement.client.CepData;
import br.com.eric.usermanagement.domain.entity.Address;
import br.com.eric.usermanagement.exception.InvalidCepException;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CepValidationService {

    private final CepClient cepClient;

    public void validateAndEnrich(Address address) {
        CepData data = cepClient.findByCep(address.getCep())
                .orElseThrow(() -> new InvalidCepException(address.getCep()));

        address.setState(data.state());
        address.setCity(data.city());
        if (StringUtils.hasText(data.street())) {
            address.setStreet(data.street());
        }
        if (StringUtils.hasText(data.district())) {
            address.setDistrict(data.district());
        }
    }
}