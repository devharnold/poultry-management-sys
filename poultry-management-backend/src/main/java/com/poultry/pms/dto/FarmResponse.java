package com.poultry.pms.dto;

import com.poultry.pms.entity.Farm;

import java.time.Instant;

public record FarmResponse(Long id, String name, String location, String contactNotes, long flockCount,
                           Instant createdAt, Instant updatedAt) {

    public static FarmResponse from(Farm farm, long flockCount) {
        return new FarmResponse(farm.getId(), farm.getName(), farm.getLocation(), farm.getContactNotes(),
                flockCount, farm.getCreatedAt(), farm.getUpdatedAt());
    }
}
