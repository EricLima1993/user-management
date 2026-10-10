package br.com.eric.usermanagement;

import br.com.eric.usermanagement.client.CepClient;
import br.com.eric.usermanagement.client.CepData;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes que sobem o contexto do Spring. Todos compartilham o mesmo contexto (mais rápido)
 * e o mesmo banco H2 em memória, por isso cada teste usa dados com e-mails únicos.
 * O ViaCEP é sempre substituído por um mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@test.local";
    protected static final String ADMIN_PASSWORD = "Admin@123";
    protected static final String PASSWORD = "Senha@1234";

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected CepClient cepClient;

    @BeforeEach
    void stubCepClient() {
        when(cepClient.findByCep(anyString())).thenAnswer(invocation -> {
            String cep = invocation.getArgument(0);
            if ("99999999".equals(cep)) {
                return Optional.empty();
            }
            return Optional.of(new CepData(cep, "Praca da Se", "Se", "Sao Paulo", "SP"));
        });
    }

    protected String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return extractText(body, "accessToken");
    }

    /** Cria um usuário pela API e devolve o corpo JSON da resposta. */
    protected String createUser(String adminToken, String name, String email, String role) throws Exception {
        return mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(name, email, role, "01001000", true)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected void updateUser(String token, long id, String json) throws Exception {
        mockMvc.perform(put("/api/v1/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    protected String userJson(String name, String email, String role, String cep, boolean main) {
        return """
                {"name":"%s","email":"%s","phone":"11999990000","password":"%s","role":"%s",
                 "addresses":[{"cep":"%s","street":"Rua Teste","number":"10","state":"SP","city":"Cidade",
                 "district":"Centro","mainAddress":%s}]}
                """.formatted(name, email, PASSWORD, role, cep, main);
    }

    protected String updateJson(String name, String status, String role, long addressId, String cep) {
        String statusPart = status == null ? "" : "\"status\":\"" + status + "\",";
        String rolePart = role == null ? "" : "\"role\":\"" + role + "\",";
        return """
                {"name":"%s",%s%s"addresses":[{"id":%d,"cep":"%s","street":"Rua Teste","number":"10",
                 "state":"SP","city":"Cidade","district":"Centro","mainAddress":true}]}
                """.formatted(name, statusPart, rolePart, addressId, cep);
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String extractText(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Campo '" + field + "' não encontrado em: " + json);
        }
        return matcher.group(1);
    }

    /** Primeiro número associado ao campo (para "id", é o id do usuário). */
    protected long extractNumber(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Campo '" + field + "' não encontrado em: " + json);
        }
        return Long.parseLong(matcher.group(1));
    }

    protected long firstAddressId(String userJson) {
        Matcher matcher = Pattern.compile("\"addresses\"\\s*:\\s*\\[\\s*\\{\\s*\"id\"\\s*:\\s*(\\d+)").matcher(userJson);
        if (!matcher.find()) {
            throw new IllegalStateException("Endereço não encontrado em: " + userJson);
        }
        return Long.parseLong(matcher.group(1));
    }
}
