package com.poultry.pms.dto;

import com.poultry.pms.entity.Flock;
import com.poultry.pms.entity.FlockStatus;

import java.time.Instant;
import java.time.LocalDate;

/** currentBirdCount = initialBirdCount - totalMortality (derived, never stored). */
public record FlockResponse(Long id, String flockCode, Long farmId, String farmName, String birdType,
                            LocalDate placementDate, int initialBirdCount, int totalMortality, int currentBirdCount,
                            FlockStatus status, String notes, Instant createdAt, Instant updatedAt) {

    public static FlockResponse from(Flock flock, int totalMortality) {
        return new FlockResponse(flock.getId(), flock.getFlockCode(), flock.getFarm().getId(), flock.getFarm().getName(),
                flock.getBirdType(), flock.getPlacementDate(), flock.getInitialBirdCount(), totalMortality,
                flock.getInitialBirdCount() - totalMortality, flock.getStatus(), flock.getNotes(),
                flock.getCreatedAt(), flock.getUpdatedAt());
    }
}
