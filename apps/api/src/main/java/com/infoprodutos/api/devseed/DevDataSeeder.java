package com.infoprodutos.api.devseed;

import com.infoprodutos.api.config.DevSeedProperties;
import com.infoprodutos.api.course.Slugifier;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseInstructor;
import com.infoprodutos.api.course.domain.CourseStatus;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.LessonAccessType;
import com.infoprodutos.api.course.domain.LessonStatus;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.domain.ModuleStatus;
import com.infoprodutos.api.course.repository.CourseInstructorRepository;
import com.infoprodutos.api.course.repository.CourseRepository;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.course.repository.ModuleRepository;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.RoleRepository;
import com.infoprodutos.api.user.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria usuários mínimos de desenvolvimento (um por papel) de forma segura:
 * - Só executa com o perfil "dev" ativo (nunca em "prod").
 * - Nenhuma senha fica hardcoded no código-fonte: vêm exclusivamente de
 *   variáveis de ambiente (DEV_SEED_*), documentadas em apps/api/.env.example.
 * - Se as variáveis não forem fornecidas, o seed correspondente é
 *   simplesmente pulado (com aviso em log), em vez de usar um valor padrão.
 * - É idempotente: não duplica usuários já existentes.
 *
 * Ver docs/SECURITY.md secao 8 e docs/DECISIONS.md.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final DevSeedProperties devSeedProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!devSeedProperties.isEnabled()) {
            log.info("Dev data seed desabilitado (app.dev-seed.enabled=false).");
            return;
        }

        seedUser("SUPER_ADMIN", "Administrador (dev)", devSeedProperties.getAdminEmail(), devSeedProperties.getAdminPassword(), RoleCode.SUPER_ADMIN);
        seedUser("INSTRUCTOR", "Professor (dev)", devSeedProperties.getInstructorEmail(), devSeedProperties.getInstructorPassword(), RoleCode.INSTRUCTOR);
        seedUser("STUDENT", "Aluno (dev)", devSeedProperties.getStudentEmail(), devSeedProperties.getStudentPassword(), RoleCode.STUDENT);
        seedSampleCourses();
        seedStudentEnrollment();
    }

    /** Curso(s) de exemplo para o professor dev poder ver o construtor curricular funcionando. */
    private void seedSampleCourses() {
        String instructorEmail = devSeedProperties.getInstructorEmail();
        if (instructorEmail == null || instructorEmail.isBlank() || courseRepository.count() > 0) {
            return;
        }
        User instructor = userRepository.findActiveByEmailIgnoreCase(instructorEmail).orElse(null);
        if (instructor == null) {
            return;
        }

        Course course = new Course("Fundamentos de Marketing Digital", Slugifier.slugify("Fundamentos de Marketing Digital"), instructor);
        course.setDescription("Um curso introdutório sobre estratégias de marketing digital para infoprodutores.");
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(Instant.now());
        course = courseRepository.save(course);
        courseInstructorRepository.save(new CourseInstructor(course, instructor, true));

        Module module1 = new Module(course, "Introdução", 0);
        module1.setStatus(ModuleStatus.PUBLISHED);
        module1 = moduleRepository.save(module1);

        Lesson lesson1 = new Lesson(module1, "Boas-vindas ao curso", 0);
        lesson1.setAccessType(LessonAccessType.FREE_PREVIEW);
        lesson1.setStatus(LessonStatus.PUBLISHED);
        lesson1.setDurationSeconds(300);
        lessonRepository.save(lesson1);

        Lesson lesson2 = new Lesson(module1, "Como este curso está organizado", 1);
        lesson2.setAccessType(LessonAccessType.ENROLLED_ONLY);
        lesson2.setStatus(LessonStatus.PUBLISHED);
        lesson2.setDurationSeconds(420);
        lessonRepository.save(lesson2);

        Module module2 = new Module(course, "Estratégia de conteúdo", 1);
        module2.setStatus(ModuleStatus.DRAFT);
        moduleRepository.save(module2);

        Course draftCourse = new Course("Copywriting para Lançamentos", Slugifier.slugify("Copywriting para Lançamentos"), instructor);
        draftCourse.setDescription("Rascunho em construção - técnicas de copywriting aplicadas a lançamentos de infoprodutos.");
        draftCourse = courseRepository.save(draftCourse);
        courseInstructorRepository.save(new CourseInstructor(draftCourse, instructor, true));

        log.info("Cursos de exemplo criados para o professor dev ({}).", instructorEmail);
    }

    /** Matricula o aluno seed no curso publicado de marketing (idempotente). */
    private void seedStudentEnrollment() {
        String studentEmail = devSeedProperties.getStudentEmail();
        String instructorEmail = devSeedProperties.getInstructorEmail();
        if (studentEmail == null || studentEmail.isBlank()) {
            return;
        }
        User student = userRepository.findActiveByEmailIgnoreCase(studentEmail).orElse(null);
        if (student == null) {
            return;
        }
        Course published = courseRepository.findAll().stream()
                .filter(c -> c.getDeletedAt() == null && c.getStatus() == CourseStatus.PUBLISHED)
                .findFirst()
                .orElse(null);
        if (published == null) {
            return;
        }
        if (enrollmentRepository.findByStudentIdAndCourseId(student.getId(), published.getId()).isPresent()) {
            return;
        }
        User granter = instructorEmail != null
                ? userRepository.findActiveByEmailIgnoreCase(instructorEmail).orElse(student)
                : student;
        enrollmentRepository.save(new Enrollment(student, published, granter.getId()));
        log.info("Matrícula seed: {} -> curso '{}'", studentEmail, published.getTitle());
    }

    private void seedUser(String label, String name, String email, String rawPassword, String roleCode) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            log.warn(
                    "Seed de usuário dev '{}' pulado: defina DEV_SEED_*_EMAIL e DEV_SEED_*_PASSWORD para habilitá-lo.",
                    label);
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Usuário dev '{}' já existe ({}), seed ignorado.", label, email);
            return;
        }
        Role role = roleRepository
                .findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Papel " + roleCode + " não encontrado - verifique o seed de roles."));

        User user = new User(name, email.toLowerCase(), passwordEncoder.encode(rawPassword));
        user.setRoles(Set.of(role));
        userRepository.save(user);
        log.info("Usuário dev '{}' criado: {}", label, email);
    }
}
