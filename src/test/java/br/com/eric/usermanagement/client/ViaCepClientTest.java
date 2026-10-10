package br.com.eric.usermanagement.client;

import br.com.eric.usermanagement.exception.CepServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa o client contra um servidor HTTP local (JDK), sem rede externa e sem Spring.
 * Cobre o parse da resposta, o "erro: true" do ViaCEP e o mapeamento de falhas.
 */
class ViaCepClientTest {

    private static final String SAMPLE = """
            {"cep":"01001-000","logradouro":"Praça da Sé","complemento":"lado ímpar","bairro":"Sé",
             "localidade":"São Paulo","uf":"SP","ibge":"3550308","gia":"1004","ddd":"11","siafi":"7107"}
            """;

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void respond(String path, int status, String body, long delayMillis) {
        server.createContext(path, exchange -> {
            try {
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }

    private ViaCepClient client(Duration readTimeout) {
        server.start();
        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/ws";
        return new ViaCepClient(new ViaCepProperties(baseUrl, Duration.ofSeconds(1), readTimeout));
    }

    @Test
    void returnsAddressDataForExistingCep() {
        respond("/ws/01001000/json", 200, SAMPLE, 0);

        Optional<CepData> result = client(Duration.ofSeconds(2)).findByCep("01001000");

        assertThat(result).isPresent();
        CepData data = result.get();
        assertThat(data.cep()).isEqualTo("01001000");
        assertThat(data.street()).isEqualTo("Praça da Sé");
        assertThat(data.district()).isEqualTo("Sé");
        assertThat(data.city()).isEqualTo("São Paulo");
        assertThat(data.state()).isEqualTo("SP");
    }

    @Test
    void returnsEmptyWhenViaCepReportsError() {
        respond("/ws/99999999/json", 200, "{\"erro\": true}", 0);

        assertThat(client(Duration.ofSeconds(2)).findByCep("99999999")).isEmpty();
    }

    @Test
    void returnsEmptyWhenViaCepAnswersBadRequest() {
        respond("/ws/12345678/json", 400, "{}", 0);

        assertThat(client(Duration.ofSeconds(2)).findByCep("12345678")).isEmpty();
    }

    @Test
    void throwsUnavailableWhenViaCepFails() {
        respond("/ws/01001000/json", 500, "{}", 0);
        ViaCepClient client = client(Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.findByCep("01001000"))
                .isInstanceOf(CepServiceUnavailableException.class);
    }

    @Test
    void throwsUnavailableWhenReadTimesOut() {
        respond("/ws/01001000/json", 200, SAMPLE, 1500);
        ViaCepClient client = client(Duration.ofMillis(300));

        assertThatThrownBy(() -> client.findByCep("01001000"))
                .isInstanceOf(CepServiceUnavailableException.class);
    }

    @Test
    void throwsUnavailableWhenConnectionIsRefused() {
        ViaCepClient client = new ViaCepClient(
                new ViaCepProperties("http://localhost:1/ws", Duration.ofSeconds(1), Duration.ofSeconds(1)));

        assertThatThrownBy(() -> client.findByCep("01001000"))
                .isInstanceOf(CepServiceUnavailableException.class);
    }
}
