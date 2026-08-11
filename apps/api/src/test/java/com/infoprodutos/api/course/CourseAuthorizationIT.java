package com.infoprodutos.api.course;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.AbstractIntegrationTest;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.RoleRepository;
import com.infoprodutos.api.user.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cobre docs/TEST_STRATEGY.md - autorização por papel e por posse (ownership)
 * para cursos/módulos/aulas (Fase 2). Requer Docker (Testcontainers) - ver
 * limitação documentada no relatório da Fase 2.
 */
class CourseAuthorizationIT extends AbstractIntegrationTest {

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
    void instructorCanCreateAndBecomesOwner() throws Exception {
        String token = createAndLogin("prof1.curso@example.com", RoleCode.INSTRUCTOR);

        String body = objectMapper.writeValueAsString(
                new CourseCreatePayload("Curso do Professor 1", null, null, java.math.BigDecimal.TEN, 10_000L));
        mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.instructors[0].name").exists());
    }

    @Test
    void studentCannotCreateCourse() throws Exception {
        String token = createAndLogin("aluno.curso@example.com", RoleCode.STUDENT);

        String body = objectMapper.writeValueAsString(
                new CourseCreatePayload("Curso Indevido", null, null, java.math.BigDecimal.TEN, 10_000L));
        mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void instructorCannotEditCourseOfAnotherInstructor() throws Exception {
        String owner = createAndLogin("dono.curso@example.com", RoleCode.INSTRUCTOR);
        String intruder = createAndLogin("intruso.curso@example.com", RoleCode.INSTRUCTOR);

        String createBody = objectMapper.writeValueAsString(
                new CourseCreatePayload("Curso Alheio", null, null, java.math.BigDecimal.TEN, 10_000L));
        var createResult = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        String courseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(
                new CourseUpdatePayload(
                        "Tentativa de edição indevida",
                        null,
                        null,
                        java.math.BigDecimal.TEN,
                        10_000L,
                        null,
                        null,
                        true,
                        null));
        mockMvc.perform(put("/api/v1/courses/" + courseId)
                        .header("Authorization", "Bearer " + intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotSeeDraftCourseOfOthers() throws Exception {
        String owner = createAndLogin("dono2.curso@example.com", RoleCode.INSTRUCTOR);
        String student = createAndLogin("aluno2.curso@example.com", RoleCode.STUDENT);

        String createBody = objectMapper.writeValueAsString(
                new CourseCreatePayload("Curso Rascunho", null, null, java.math.BigDecimal.TEN, 10_000L));
        var createResult = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        String courseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/courses/" + courseId).header("Authorization", "Bearer " + student))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanSeePublishedCourse() throws Exception {
        String owner = createAndLogin("dono3.curso@example.com", RoleCode.INSTRUCTOR);
        String student = createAndLogin("aluno3.curso@example.com", RoleCode.STUDENT);

        String createBody = objectMapper.writeValueAsString(
                new CourseCreatePayload("Curso Publicado", null, null, java.math.BigDecimal.TEN, 10_000L));
        var createResult = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        String courseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish").header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/courses/" + courseId).header("Authorization", "Bearer " + student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void moduleAndLessonCrudFlow_reorderAndPublish() throws Exception {
        String owner = createAndLogin("dono.curriculo@example.com", RoleCode.INSTRUCTOR);

        String createBody = objectMapper.writeValueAsString(
                new CourseCreatePayload("Curso com Currículo", null, null, java.math.BigDecimal.TEN, 10_000L));
        var createResult = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        String courseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        var module1Result = mockMvc.perform(post("/api/v1/courses/" + courseId + "/modules")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ModulePayload("Módulo 1", null))))
                .andExpect(status().isCreated())
                .andReturn();
        String module1Id = objectMapper.readTree(module1Result.getResponse().getContentAsString()).get("id").asText();

        var module2Result = mockMvc.perform(post("/api/v1/courses/" + courseId + "/modules")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ModulePayload("Módulo 2", null))))
                .andExpect(status().isCreated())
                .andReturn();
        String module2Id = objectMapper.readTree(module2Result.getResponse().getContentAsString()).get("id").asText();

        // Reordena: módulo 2 passa a vir primeiro.
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/modules/reorder")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReorderPayload(java.util.List.of(module2Id, module1Id)))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/courses/" + courseId + "/modules").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(module2Id))
                .andExpect(jsonPath("$[1].id").value(module1Id));

        var lessonResult = mockMvc.perform(post("/api/v1/modules/" + module1Id + "/lessons")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LessonPayload("Aula 1", null, 60, "FREE_PREVIEW"))))
                .andExpect(status().isCreated())
                .andReturn();
        String lessonId = objectMapper.readTree(lessonResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/lessons/" + lessonId + "/publish").header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/modules/" + module1Id + "/lessons").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    private String createAndLogin(String email, String roleCode) throws Exception {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User("Usuario " + roleCode, email, passwordEncoder.encode("SenhaForte123"));
        user.setRoles(Set.of(role));
        userRepository.save(user);

        String loginBody = objectMapper.writeValueAsString(new LoginPayload(email, "SenhaForte123"));
        var result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private record LoginPayload(String email, String password) {}

    private record CourseCreatePayload(
            String title, String slug, String description, java.math.BigDecimal workloadHours, Long priceCents) {}

    private record CourseUpdatePayload(
            String title,
            String description,
            String coverImageUrl,
            java.math.BigDecimal workloadHours,
            Long priceCents,
            java.math.BigDecimal minCompletionPercentage,
            java.math.BigDecimal minPassingScore,
            boolean certificateEnabled,
            Integer maxQuizAttempts) {}

    private record ModulePayload(String title, String description) {}

    private record LessonPayload(String title, String description, Integer durationSeconds, String accessType) {}

    private record ReorderPayload(java.util.List<String> orderedIds) {}
}
