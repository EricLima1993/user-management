package br.com.eric.usermanagement.controller;

import br.com.eric.usermanagement.BaseIntegrationTest;
import br.com.eric.usermanagement.exception.CepServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiTest extends BaseIntegrationTest {

    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

    private String adminToken;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    // ---------- apoio ----------

    private String newUser(String prefix) throws Exception {
        return createUser(adminToken, "Usuario " + prefix, uniqueEmail(prefix), "USER");
    }

    /** Cria um usuário comum e devolve {token, id, idDoEndereço}. */
    private String[] newUserWithToken(String prefix) throws Exception {
        String email = uniqueEmail(prefix);
        String body = createUser(adminToken, "Usuario " + prefix, email, "USER");
        return new String[]{login(email, PASSWORD),
                String.valueOf(extractNumber(body, "id")), String.valueOf(firstAddressId(body))};
    }

    // ---------- criação ----------

    @Test
    void adminCreatesUserWithAddressEnrichedByViaCep() throws Exception {
        String email = uniqueEmail("maria");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Maria Silva", email.toUpperCase(), "USER", "01001000", true)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.createdBy").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.addresses[0].city").value("Sao Paulo"))
                .andExpect(jsonPath("$.addresses[0].street").value("Praca da Se"))
                .andExpect(jsonPath("$.addresses[0].mainAddress").value(true));
    }

    @Test
    void userCannotCreateUsers() throws Exception {
        String token = newUserWithToken("comum")[0];

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Outro", uniqueEmail("outro"), "USER", "01001000", true)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Anonimo", uniqueEmail("anonimo"), "USER", "01001000", true)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.instance").value("/api/v1/users"));
    }

    @Test
    void duplicatedEmailIsRejectedEvenIfTheCaseDiffers() throws Exception {
        String email = uniqueEmail("duplicado");
        createUser(adminToken, "Primeiro", email, "USER");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Segundo", email.toUpperCase(), "USER", "01001000", true)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    void invalidPayloadReturnsTheListOfFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalido\",\"password\":\"123\",\"role\":\"USER\",\"addresses\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.errors[?(@.field=='name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='addresses')]").exists());
    }

    @Test
    void malformedCepIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Cep Curto", uniqueEmail("cepcurto"), "USER", "123", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='addresses[0].cep')]").exists());
    }

    @Test
    void unknownCepIsRejectedWith422() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Cep Ruim", uniqueEmail("cepruim"), "USER", "99999999", true)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.detail").value(containsString("99999999")));
    }

    @Test
    void viaCepOutageReturns503() throws Exception {
        doThrow(new CepServiceUnavailableException(new RuntimeException("fora do ar")))
                .when(cepClient).findByCep(anyString());

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Sem Cep", uniqueEmail("semcep"), "USER", "01001000", true)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    void moreThanOneMainAddressIsRejectedWith422() throws Exception {
        String body = """
                {"name":"Dois Principais","email":"%s","password":"%s","role":"USER","addresses":[
                 {"cep":"01001000","street":"Rua A","number":"1","state":"SP","city":"Cidade","mainAddress":true},
                 {"cep":"01001000","street":"Rua B","number":"2","state":"SP","city":"Cidade","mainAddress":true}]}
                """.formatted(uniqueEmail("doisprincipais"), PASSWORD);

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- consulta ----------

    @Test
    void userReadsOnlyTheirOwnData() throws Exception {
        String[] ana = newUserWithToken("ana");
        String bruno = newUser("bruno");

        mockMvc.perform(get("/api/v1/users/{id}", ana[1]).header(HttpHeaders.AUTHORIZATION, bearer(ana[0])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addresses").isArray());

        mockMvc.perform(get("/api/v1/users/{id}", extractNumber(bruno, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana[0])))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    void adminReadsAnyUserAndGetsNotFoundForUnknownIds() throws Exception {
        String created = newUser("qualquer");

        mockMvc.perform(get("/api/v1/users/{id}", extractNumber(created, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/{id}", 999999999L).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    // ---------- atualização ----------

    @Test
    void userUpdatesTheirOwnName() throws Exception {
        String email = uniqueEmail("renomear");
        String created = createUser(adminToken, "Nome Antigo", email, "USER");
        String token = login(email, PASSWORD);

        mockMvc.perform(put("/api/v1/users/{id}", extractNumber(created, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Nome Novo", null, null, firstAddressId(created), "01001000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nome Novo"))
                .andExpect(jsonPath("$.updatedBy").value(email));
    }

    @Test
    void userCannotPromoteThemselvesOrChangeTheirStatus() throws Exception {
        String[] user = newUserWithToken("promover");

        mockMvc.perform(put("/api/v1/users/{id}", user[1])
                        .header(HttpHeaders.AUTHORIZATION, bearer(user[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Nome", null, "ADMIN", Long.parseLong(user[2]), "01001000")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/users/{id}", user[1])
                        .header(HttpHeaders.AUTHORIZATION, bearer(user[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Nome", "INACTIVE", null, Long.parseLong(user[2]), "01001000")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanChangeRoleAndStatus() throws Exception {
        String created = newUser("promovido");

        mockMvc.perform(put("/api/v1/users/{id}", extractNumber(created, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Promovido", "INACTIVE", "ADMIN", firstAddressId(created), "01001000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void updateWithUnknownCepIsRejected() throws Exception {
        String created = newUser("cepupdate");

        mockMvc.perform(put("/api/v1/users/{id}", extractNumber(created, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Nome", null, null, firstAddressId(created), "99999999")))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- exclusão lógica ----------

    @Test
    void userCannotDeleteUsers() throws Exception {
        String[] user = newUserWithToken("naoexclui");

        mockMvc.perform(delete("/api/v1/users/{id}", user[1]).header(HttpHeaders.AUTHORIZATION, bearer(user[0])))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletedUserDisappearsCannotLoginAndKeepsTheEmailReserved() throws Exception {
        String email = uniqueEmail("excluido");
        String created = createUser(adminToken, "Excluido", email, "USER");
        long id = extractNumber(created, "id");

        mockMvc.perform(delete("/api/v1/users/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Recriado", email, "USER", "01001000", true)))
                .andExpect(status().isConflict());
    }

    // ---------- listagem ----------

    @Test
    void adminListsWithSearchAndSorting() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        createUser(adminToken, "Ana " + tag, "ana-" + tag + "@test.com", "USER");
        createUser(adminToken, "Bruno " + tag, "bruno-" + tag + "@test.com", "USER");

        mockMvc.perform(get("/api/v1/users")
                        .param("search", tag)
                        .param("sort", "name,desc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Bruno " + tag))
                .andExpect(jsonPath("$.content[1].name").value("Ana " + tag))
                .andExpect(jsonPath("$.content[0].city").value("Sao Paulo"))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void listFiltersByStatus() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        createUser(adminToken, "Ativo " + tag, "ativo-" + tag + "@test.com", "USER");
        String inactive = createUser(adminToken, "Inativo " + tag, "inativo-" + tag + "@test.com", "USER");
        updateUser(adminToken, extractNumber(inactive, "id"),
                updateJson("Inativo " + tag, "INACTIVE", null, firstAddressId(inactive), "01001000"));

        mockMvc.perform(get("/api/v1/users")
                        .param("search", tag)
                        .param("status", "INACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Inativo " + tag));
    }

    @Test
    void userCannotListUsers() throws Exception {
        String token = newUserWithToken("semlista")[0];

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidSortFieldAndInvalidStatusReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("sort", "password,asc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));

        mockMvc.perform(get("/api/v1/users").param("status", "XYZ")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pageSizeIsLimitedTo100() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("size", "1000")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }
}
