package com.poultry.pms.dto;

import com.poultry.pms.entity.MortalityRecord;

import java.time.Instant;
import java.time.LocalDate;

public record MortalityResponse(Long id, Long flockId, String flockCode, LocalDate recordDate, int count,
                                String reason, String clientOperationId, Instant createdAt) {

    public static MortalityResponse from(MortalityRecord r) {
        return new MortalityResponse(r.getId(), r.getFlock().getId(), r.getFlock().getFlockCode(), r.getRecordDate(),
                r.getDeathCount(), r.getReason(), r.getClientOperationId(), r.getCreatedAt());
    }
}
