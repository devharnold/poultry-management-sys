package com.poultry.pms.dto;

import com.poultry.pms.entity.WeightRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WeightResponse(Long id, Long flockId, LocalDate recordDate, BigDecimal averageBirdWeightKg,
                             int sampledBirdCount, String notes, Instant createdAt) {

    public static WeightResponse from(WeightRecord w) {
        return new WeightResponse(w.getId(), w.getFlock().getId(), w.getRecordDate(), w.getAverageBirdWeightKg(),
                w.getSampledBirdCount(), w.getNotes(), w.getCreatedAt());
    }
}
