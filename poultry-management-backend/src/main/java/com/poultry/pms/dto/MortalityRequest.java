package com.poultry.pms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * clientOperationId (optional): a UUID created by the browser. If the same id arrives twice
 * (a retry after a lost connection) the server returns the original record instead of creating a duplicate.
 */
public record MortalityRequest(
        @NotNull(message = "Flock is required") Long flockId,
        @NotNull(message = "Date is required") LocalDate recordDate,
        @NotNull(message = "Number of deaths is required") @Min(value = 1, message = "Number of deaths must be at least 1") Integer count,
        @Size(max = 500, message = "Reason/notes must be at most 500 characters") String reason,
        @Pattern(regexp = "^[A-Za-z0-9-]{8,64}$", message = "clientOperationId must be 8-64 letters, digits or hyphens") String clientOperationId) {
}
