package com.poultry.pms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** flockId is optional, but feed usage without a flock cannot be used for that flock's feed conversion ratio. */
public record UsageRequest(
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
        @Digits(integer = 9, fraction = 3, message = "Use at most 3 decimal places") BigDecimal quantity,
        LocalDate movementDate,
        Long flockId,
        @Size(max = 500, message = "Notes must be at most 500 characters") String notes,
        @Pattern(regexp = "^[A-Za-z0-9-]{8,64}$", message = "clientOperationId must be 8-64 letters, digits or hyphens") String clientOperationId) {
}
