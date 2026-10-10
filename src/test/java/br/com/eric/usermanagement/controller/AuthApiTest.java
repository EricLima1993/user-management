package br.com.eric.usermanagement.controller;

import br.com.eric.usermanagement.BaseIntegrationTest;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends BaseIntegrationTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    private String loginBody(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    /** Faz um login que deve falhar com 401 e devolve o corpo da resposta. */
    private String failedLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String token(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(ADMIN_EMAIL)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", "ADMIN")
                .claim("userId", 1L)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void loginReturnsABearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(300));
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        String wrongPassword = failedLogin(ADMIN_EMAIL, "senha-errada");
        String unknownEmail = failedLogin("ninguem@test.com", "qualquer-coisa");

        assertThat(extractText(wrongPassword, "detail")).isEqualTo(extractText(unknownEmail, "detail"));
        assertThat(extractText(wrongPassword, "title")).isEqualTo(extractText(unknownEmail, "title"));
    }

    @Test
    void inactiveUserCannotLogin() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = uniqueEmail("inativo");
        String created = createUser(adminToken, "Usuario Inativo", email, "USER");
        updateUser(adminToken, extractNumber(created, "id"),
                updateJson("Usuario Inativo", "INACTIVE", null, firstAddressId(created), "01001000"));

        failedLogin(email, PASSWORD);
    }

    @Test
    void invalidLoginPayloadIsRejectedWithValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void protectedRouteWithoutTokenReturnsProblemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/api/v1/users/1"));
    }

    @Test
    void malformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/1").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() throws Exception {
        SecretKeySpec otherKey = new SecretKeySpec(
                "another-secret-key-with-more-than-32-characters".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));
        Instant now = Instant.now();

        String forged = token(otherEncoder, now, now.plus(5, ChronoUnit.MINUTES));

        mockMvc.perform(get("/api/v1/users/1").header(HttpHeaders.AUTHORIZATION, bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant now = Instant.now();

        String expired = token(jwtEncoder, now.minus(10, ChronoUnit.MINUTES), now.minus(5, ChronoUnit.MINUTES));

        mockMvc.perform(get("/api/v1/users/1").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized());
    }
}
