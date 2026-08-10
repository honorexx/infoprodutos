package com.infoprodutos.api.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record PackageUpsertRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 220) String slug,
        String description,
        @NotNull @Min(1) Long priceCents,
        Boolean active,
        @NotEmpty List<UUID> courseIds) {}
