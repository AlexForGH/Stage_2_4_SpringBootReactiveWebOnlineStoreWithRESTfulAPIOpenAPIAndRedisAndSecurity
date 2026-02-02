package org.pl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserBalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn401_WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/balance/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnBalance_WhenValidJwtProvided() throws Exception {
        mockMvc.perform(get("/api/balance/1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.balance").exists());
    }

    /**
     * Тест с issuer от localhost (как браузер видит Keycloak)
     */
    @Test
    void shouldAcceptToken_WithLocalhostIssuer() throws Exception {
        mockMvc.perform(get("/api/balance/1")
                        .with(jwt().jwt(builder -> builder
                                .issuer("http://localhost:8080/realms/web-store")
                                .subject("test-user")
                                .claim("preferred_username", "testuser")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    /**
     * Тест с issuer от keycloak-service (как сервисы видят Keycloak внутри Docker)
     */
    @Test
    void shouldAcceptToken_WithDockerInternalIssuer() throws Exception {
        mockMvc.perform(get("/api/balance/1")
                        .with(jwt().jwt(builder -> builder
                                .issuer("http://keycloak-service:8080/realms/web-store")
                                .subject("test-user")
                                .claim("preferred_username", "testuser")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void shouldReturnBalanceForDifferentUsers() throws Exception {
        mockMvc.perform(get("/api/balance/1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void shouldReturn401_WhenUpdatingBalanceWithoutToken() throws Exception {
        mockMvc.perform(put("/api/balance/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": 500.00}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldUpdateBalance_WhenValidJwtProvided() throws Exception {
        mockMvc.perform(put("/api/balance/1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": 750.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.balance").exists());
    }

    @Test
    void shouldUpdateBalance_WithLocalhostIssuerToken() throws Exception {
        mockMvc.perform(put("/api/balance/1")
                        .with(jwt().jwt(builder -> builder
                                .issuer("http://localhost:8080/realms/web-store")
                                .subject("service-account")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": 1000.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateBalance_WithDockerInternalIssuerToken() throws Exception {
        mockMvc.perform(put("/api/balance/1")
                        .with(jwt().jwt(builder -> builder
                                .issuer("http://keycloak-service:8080/realms/web-store")
                                .subject("service-account")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": 1000.00}"))
                .andExpect(status().isOk());
    }
}
