package com.infoprodutos.api.commerce;

import com.infoprodutos.api.commerce.domain.ProductPackage;
import com.infoprodutos.api.commerce.dto.PackageResponse;
import com.infoprodutos.api.commerce.dto.PackageUpsertRequest;
import com.infoprodutos.api.commerce.repository.ProductPackageRepository;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ConflictException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.Slugifier;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseStatus;
import com.infoprodutos.api.course.repository.CourseRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final ProductPackageRepository packageRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<PackageResponse> listActive() {
        return packageRepository.findAllActiveWithCourses().stream().map(PackageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PackageResponse> listAllAdmin() {
        return packageRepository.findAllNotDeletedWithCourses().stream().map(PackageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PackageResponse getActive(UUID id) {
        return PackageResponse.from(findActiveOrThrow(id));
    }

    @Transactional
    public PackageResponse create(PackageUpsertRequest request) {
        String slug = resolveSlug(request.slug(), request.title(), null);
        ProductPackage pkg = new ProductPackage(request.title().trim(), slug, request.priceCents());
        pkg.setDescription(request.description());
        pkg.setCurrency("BRL");
        pkg.setActive(request.active() == null || request.active());
        pkg.setCourses(resolveCourses(request.courseIds()));
        return PackageResponse.from(packageRepository.save(pkg));
    }

    @Transactional
    public PackageResponse update(UUID id, PackageUpsertRequest request) {
        ProductPackage pkg = findNotDeletedOrThrow(id);
        String slug = resolveSlug(request.slug(), request.title(), pkg.getId());
        pkg.setTitle(request.title().trim());
        pkg.setSlug(slug);
        pkg.setDescription(request.description());
        pkg.setPriceCents(request.priceCents());
        if (request.active() != null) {
            pkg.setActive(request.active());
        }
        pkg.getCourses().clear();
        pkg.getCourses().addAll(resolveCourses(request.courseIds()));
        return PackageResponse.from(packageRepository.save(pkg));
    }

    @Transactional
    public void softDelete(UUID id) {
        ProductPackage pkg = findNotDeletedOrThrow(id);
        pkg.setDeletedAt(Instant.now());
        pkg.setActive(false);
        packageRepository.save(pkg);
    }

    public ProductPackage findActiveOrThrow(UUID id) {
        return packageRepository
                .findActiveByIdWithCourses(id)
                .filter(p -> p.isActive() && !p.isDeleted())
                .orElseThrow(() -> new NotFoundException("Pacote não encontrado."));
    }

    private ProductPackage findNotDeletedOrThrow(UUID id) {
        return packageRepository
                .findActiveByIdWithCourses(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new NotFoundException("Pacote não encontrado."));
    }

    private Set<Course> resolveCourses(List<UUID> courseIds) {
        Set<Course> courses = new HashSet<>();
        for (UUID courseId : courseIds) {
            Course course = courseRepository
                    .findActiveById(courseId)
                    .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + courseId));
            if (course.getStatus() == CourseStatus.ARCHIVED) {
                throw new BadRequestException("Não é possível incluir curso arquivado no pacote.");
            }
            courses.add(course);
        }
        if (courses.isEmpty()) {
            throw new BadRequestException("Pacote precisa de ao menos um curso.");
        }
        return courses;
    }

    private String resolveSlug(String rawSlug, String title, UUID currentId) {
        String slug = (rawSlug == null || rawSlug.isBlank())
                ? Slugifier.slugify(title)
                : Slugifier.slugify(rawSlug);
        if (slug.isBlank()) {
            throw new BadRequestException("Não foi possível gerar um slug para o pacote.");
        }
        boolean taken = packageRepository.existsBySlugAndDeletedAtIsNull(slug);
        if (taken) {
            if (currentId == null) {
                throw new ConflictException("Já existe um pacote com este slug.");
            }
            ProductPackage existing = packageRepository.findAllNotDeletedWithCourses().stream()
                    .filter(p -> p.getSlug().equals(slug))
                    .findFirst()
                    .orElse(null);
            if (existing != null && !existing.getId().equals(currentId)) {
                throw new ConflictException("Já existe um pacote com este slug.");
            }
        }
        return slug;
    }
}
