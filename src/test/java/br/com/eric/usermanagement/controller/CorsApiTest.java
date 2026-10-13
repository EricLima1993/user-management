package br.com.eric.usermanagement.controller;

import br.com.eric.usermanagement.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsApiTest extends BaseIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";

    @Test
    void preflightFromAllowedOriginIsAccepted() throws Exception {
        mockMvc.perform(options("/api/v1/users")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")));
    }

    @Test
    void preflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/users")
                        .header(HttpHeaders.ORIGIN, "http://site-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualRequestFromAllowedOriginReceivesTheCorsHeader() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }
}
