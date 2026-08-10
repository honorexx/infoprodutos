package com.infoprodutos.api.commerce;

import com.infoprodutos.api.commerce.dto.PackageResponse;
import com.infoprodutos.api.commerce.dto.PackageUpsertRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping("/api/v1/packages")
    public List<PackageResponse> listActive() {
        return packageService.listActive();
    }

    @GetMapping("/api/v1/packages/{id}")
    public PackageResponse get(@PathVariable UUID id) {
        return packageService.getActive(id);
    }

    @GetMapping("/api/v1/admin/packages")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<PackageResponse> listAdmin() {
        return packageService.listAllAdmin();
    }

    @PostMapping("/api/v1/admin/packages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PackageResponse create(@Valid @RequestBody PackageUpsertRequest request) {
        return packageService.create(request);
    }

    @PutMapping("/api/v1/admin/packages/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PackageResponse update(@PathVariable UUID id, @Valid @RequestBody PackageUpsertRequest request) {
        return packageService.update(id, request);
    }

    @DeleteMapping("/api/v1/admin/packages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(@PathVariable UUID id) {
        packageService.softDelete(id);
    }
}
