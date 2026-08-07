package com.infoprodutos.api.auth;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenLoginFlow() throws Exception {
        String email = "novo.aluno@example.com";
        String registerBody =
                objectMapper.writeValueAsString(new RegisterPayload("Aluno Teste", email, "SenhaForte123"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));

        String loginBody = objectMapper.writeValueAsString(new LoginPayload(email, "SenhaForte123"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    void registerWithDuplicateEmail_returnsConflict() throws Exception {
        String email = "duplicado@example.com";
        String body = objectMapper.writeValueAsString(new RegisterPayload("Aluno", email, "SenhaForte123"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPassword_returnsGenericUnauthorized() throws Exception {
        String email = "aluno.senha@example.com";
        String registerBody =
                objectMapper.writeValueAsString(new RegisterPayload("Aluno", email, "SenhaForte123"));
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk());

        String wrongLoginBody = objectMapper.writeValueAsString(new LoginPayload(email, "SenhaErrada999"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrongLoginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void meEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndOldCookieBecomesInvalid() throws Exception {
        String email = "refresh.flow@example.com";
        String registerBody =
                objectMapper.writeValueAsString(new RegisterPayload("Aluno", email, "SenhaForte123"));
        MvcResult registerResult = mockMvc.perform(
                        post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = registerResult.getResponse().getCookie("refresh_token");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn();
        Cookie newCookie = refreshResult.getResponse().getCookie("refresh_token");

        // O cookie antigo (já rotacionado) não pode mais ser usado.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie)).andExpect(status().isUnauthorized());

        // O novo cookie funciona normalmente.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(newCookie)).andExpect(status().isOk());
    }

    @Test
    void logoutInvalidatesRefreshToken() throws Exception {
        String email = "logout.flow@example.com";
        String registerBody =
                objectMapper.writeValueAsString(new RegisterPayload("Aluno", email, "SenhaForte123"));
        MvcResult registerResult = mockMvc.perform(
                        post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = registerResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie)).andExpect(status().isUnauthorized());
    }

    private record RegisterPayload(String name, String email, String password) {}

    private record LoginPayload(String email, String password) {}
}
