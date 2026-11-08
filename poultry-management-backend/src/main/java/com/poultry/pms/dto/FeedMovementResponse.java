package com.poultry.pms.dto;

import com.poultry.pms.entity.FeedMovement;
import com.poultry.pms.entity.MovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record FeedMovementResponse(Long id, Long feedItemId, String feedItemName, MovementType movementType,
                                   BigDecimal quantity, BigDecimal quantityAfter, String unit, LocalDate movementDate,
                                   Long flockId, String flockCode, String notes, String clientOperationId,
                                   Instant createdAt) {

    public static FeedMovementResponse from(FeedMovement m) {
        return new FeedMovementResponse(m.getId(), m.getFeedItem().getId(), m.getFeedItem().getName(),
                m.getMovementType(), m.getQuantity(), m.getQuantityAfter(), m.getFeedItem().getUnit(),
                m.getMovementDate(), m.getFlock() == null ? null : m.getFlock().getId(),
                m.getFlock() == null ? null : m.getFlock().getFlockCode(), m.getNotes(),
                m.getClientOperationId(), m.getCreatedAt());
    }
}
