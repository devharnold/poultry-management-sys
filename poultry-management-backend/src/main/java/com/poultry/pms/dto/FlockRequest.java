package com.poultry.pms.dto;

import com.poultry.pms.entity.FlockStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FlockRequest(
        @NotBlank(message = "Flock/batch code is required") @Size(max = 40, message = "Flock code must be at most 40 characters") String flockCode,
        @NotNull(message = "Farm is required") Long farmId,
        @NotBlank(message = "Bird type/breed is required") @Size(max = 80, message = "Bird type must be at most 80 characters") String birdType,
        @NotNull(message = "Placement date is required") LocalDate placementDate,
        @NotNull(message = "Initial bird count is required")
        @Min(value = 1, message = "Initial bird count must be at least 1")
        @Max(value = 1000000, message = "Initial bird count is too large") Integer initialBirdCount,
        FlockStatus status,
        @Size(max = 1000, message = "Notes must be at most 1000 characters") String notes) {
}
