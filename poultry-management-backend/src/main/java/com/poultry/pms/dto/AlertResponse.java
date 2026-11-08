package com.poultry.pms.dto;

import com.poultry.pms.entity.Alert;
import com.poultry.pms.entity.AlertSeverity;
import com.poultry.pms.entity.AlertStatus;
import com.poultry.pms.entity.AlertType;

import java.time.Instant;

/** source is always PROTOTYPE_GENERATED: alerts are created by this app's rules and nothing is sent by SMS/email. */
public record AlertResponse(Long id, AlertType alertType, AlertSeverity severity, AlertStatus status, String message,
                            String relatedEntityType, Long relatedEntityId, String source,
                            Instant createdAt, Instant resolvedAt) {

    public static final String SOURCE = "PROTOTYPE_GENERATED";

    public static AlertResponse from(Alert a) {
        return new AlertResponse(a.getId(), a.getAlertType(), a.getSeverity(), a.getStatus(), a.getMessage(),
                a.getRelatedEntityType(), a.getRelatedEntityId(), SOURCE, a.getCreatedAt(), a.getResolvedAt());
    }
}
