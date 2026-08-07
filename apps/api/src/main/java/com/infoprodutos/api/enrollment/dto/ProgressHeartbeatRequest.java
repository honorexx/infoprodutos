package com.infoprodutos.api.enrollment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProgressHeartbeatRequest(@NotNull @Min(0) Integer positionSeconds) {}
