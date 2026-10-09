package br.com.eric.usermanagement.client;

import br.com.eric.usermanagement.exception.CepServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.Optional;

@Slf4j
@Component
public class ViaCepClient implements CepClient {

    private final RestClient restClient;

    public ViaCepClient(ViaCepProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build());
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @Cacheable(cacheNames = "viacep", key = "#cep")
    public Optional<CepData> findByCep(String cep) {
        log.info("Consultando ViaCEP: {}", cep);
        try {
            ViaCepResponse response = restClient.get()
                    .uri("/{cep}/json", cep)
                    .retrieve()
                    .body(ViaCepResponse.class);

            if (response == null || Boolean.TRUE.equals(response.erro())) {
                return Optional.empty();
            }
            return Optional.of(new CepData(cep, response.logradouro(), response.bairro(),
                    response.localidade(), response.uf()));
        } catch (HttpClientErrorException ex) {
            int status = ex.getStatusCode().value();
            if (status == 400 || status == 404) {
                return Optional.empty();
            }
            throw new CepServiceUnavailableException(ex);
        } catch (RestClientException ex) {
            throw new CepServiceUnavailableException(ex);
        }
    }
}
