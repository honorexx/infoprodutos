package com.infoprodutos.api.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.AbstractIntegrationTest;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.domain.UserStatus;
import com.infoprodutos.api.user.repository.RoleRepository;
import com.infoprodutos.api.user.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cobre docs/TEST_STRATEGY.md secao 5 (Autorização por papel) para os
 * endpoints administrativos de usuários implementados na Fase 1.
 */
class AuthorizationIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void studentCannotListUsers() throws Exception {
        String accessToken = registerAndLogin("aluno.autorizacao@example.com");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointIsPublicAndHealthy() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void superAdminCanListUsers() throws Exception {
        String accessToken = createAndLoginSuperAdmin("admin.autorizacao@example.com");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void blockedUserCannotLogin() throws Exception {
        String email = "bloqueado.autorizacao@example.com";
        registerAndLogin(email);

        User user = userRepository.findActiveByEmailIgnoreCase(email).orElseThrow();
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);

        String loginBody = objectMapper.writeValueAsString(new LoginPayload(email, "SenhaForte123"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterPayload("Usuario Teste", email, "SenhaForte123"));
        var result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createAndLoginSuperAdmin(String email) throws Exception {
        Role superAdminRole = roleRepository.findByCode(RoleCode.SUPER_ADMIN).orElseThrow();
        User admin = new User("Admin Teste", email, passwordEncoder.encode("SenhaForte123"));
        admin.setRoles(Set.of(superAdminRole));
        userRepository.save(admin);

        String loginBody = objectMapper.writeValueAsString(new LoginPayload(email, "SenhaForte123"));
        var result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private record RegisterPayload(String name, String email, String password) {}

    private record LoginPayload(String email, String password) {}
}
