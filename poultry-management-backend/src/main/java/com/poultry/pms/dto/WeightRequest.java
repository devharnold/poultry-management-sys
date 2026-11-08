package com.poultry.pms.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeightRequest(
        @NotNull(message = "Date is required") LocalDate recordDate,
        @NotNull(message = "Average weight is required")
        @DecimalMin(value = "0.001", message = "Average weight must be greater than zero")
        @DecimalMax(value = "20.000", message = "Average weight looks too large (kg per bird)")
        @Digits(integer = 3, fraction = 3, message = "Use at most 3 decimal places") BigDecimal averageBirdWeightKg,
        @NotNull(message = "Sample size is required") @Min(value = 1, message = "Sample size must be at least 1") Integer sampledBirdCount,
        @Size(max = 500, message = "Notes must be at most 500 characters") String notes) {
}
