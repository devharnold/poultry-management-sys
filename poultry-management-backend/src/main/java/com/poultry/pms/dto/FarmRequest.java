package com.poultry.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FarmRequest(
        @NotBlank(message = "Farm name is required") @Size(max = 120, message = "Farm name must be at most 120 characters") String name,
        @Size(max = 200, message = "Location must be at most 200 characters") String location,
        @Size(max = 1000, message = "Contact/notes must be at most 1000 characters") String contactNotes) {
}
