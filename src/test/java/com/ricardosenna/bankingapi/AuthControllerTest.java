package com.ricardosenna.bankingapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricardosenna.bankingapi.dto.LoginRequest;
import com.ricardosenna.bankingapi.dto.RegisterRequest;
import com.ricardosenna.bankingapi.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Test
    void shouldRegisterUser() throws Exception {
        var request = new RegisterRequest("Ricardo Senna", "ricardo@example.com", "SenhaSegura123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ricardo Senna"))
                .andExpect(jsonPath("$.email").value("ricardo@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldRejectRegistrationWithInvalidEmail() throws Exception {
        var request = new RegisterRequest("Ricardo", "invalid-email", "SenhaSegura123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void shouldRejectRegistrationWithShortPassword() throws Exception {
        var request = new RegisterRequest("Ricardo", "ricardo@example.com", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        // First register
        authService.register(new RegisterRequest("Ricardo Senna", "ricardo@example.com", "SenhaSegura123"));

        // Then login
        var loginRequest = new LoginRequest("ricardo@example.com", "SenhaSegura123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {
        authService.register(new RegisterRequest("Ricardo Senna", "ricardo@example.com", "SenhaSegura123"));

        var loginRequest = new LoginRequest("ricardo@example.com", "WrongPassword123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAccessWithoutToken() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
