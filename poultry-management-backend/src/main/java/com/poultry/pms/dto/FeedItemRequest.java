package com.poultry.pms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** All quantities are kilograms. openingQuantity is only used when creating (it becomes the first STOCK_IN movement). */
public record FeedItemRequest(
        @NotBlank(message = "Feed name is required") @Size(max = 120, message = "Name must be at most 120 characters") String name,
        @NotBlank(message = "Feed type is required") @Size(max = 80, message = "Feed type must be at most 80 characters") String feedType,
        @NotNull(message = "Reorder threshold is required")
        @DecimalMin(value = "0.000", message = "Reorder threshold cannot be negative")
        @Digits(integer = 9, fraction = 3, message = "Use at most 3 decimal places") BigDecimal reorderThreshold,
        @DecimalMin(value = "0.000", message = "Opening quantity cannot be negative")
        @Digits(integer = 9, fraction = 3, message = "Use at most 3 decimal places") BigDecimal openingQuantity,
        @Size(max = 120, message = "Supplier must be at most 120 characters") String supplier,
        @Size(max = 1000, message = "Notes must be at most 1000 characters") String notes) {
}
