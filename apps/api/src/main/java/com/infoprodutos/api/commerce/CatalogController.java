package com.infoprodutos.api.commerce;

import com.infoprodutos.api.commerce.dto.PackageResponse;
import com.infoprodutos.api.course.dto.CourseSummaryResponse;
import com.infoprodutos.api.course.domain.CourseStatus;
import com.infoprodutos.api.course.repository.CourseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CourseRepository courseRepository;
    private final PackageService packageService;

    @GetMapping("/courses")
    public List<CourseSummaryResponse> publishedCourses() {
        return courseRepository
                .findAllActiveByStatus(
                        CourseStatus.PUBLISHED, PageRequest.of(0, 100, Sort.by("title").ascending()))
                .map(CourseSummaryResponse::from)
                .getContent();
    }

    @GetMapping("/packages")
    public List<PackageResponse> activePackages() {
        return packageService.listActive();
    }
}
