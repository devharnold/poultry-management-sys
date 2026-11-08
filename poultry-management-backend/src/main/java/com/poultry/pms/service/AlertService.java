package com.poultry.pms.service;

import com.poultry.pms.config.AppProperties;
import com.poultry.pms.dto.AlertResponse;
import com.poultry.pms.entity.Alert;
import com.poultry.pms.entity.AlertSeverity;
import com.poultry.pms.entity.AlertStatus;
import com.poultry.pms.entity.AlertType;
import com.poultry.pms.entity.FeedItem;
import com.poultry.pms.entity.Flock;
import com.poultry.pms.exception.NotFoundException;
import com.poultry.pms.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Creates, updates and resolves the prototype's alerts.
 * Rule 1 (HIGH_MORTALITY): deaths on one date >= threshold % of the flock's initial birds.
 * Rule 2 (LOW_STOCK): quantity on hand <= reorder threshold; it resolves itself when stock is replenished.
 * Alerts are stored and displayed only - no SMS or email is sent.
 */
@Service
@Transactional
public class AlertService {

    private static final Comparator<Alert> NEWEST_FIRST =
            Comparator.comparing(Alert::getCreatedAt).thenComparing(Alert::getId).reversed();

    private final AlertRepository alerts;
    private final AppProperties properties;

    public AlertService(AlertRepository alerts, AppProperties properties) {
        this.alerts = alerts;
        this.properties = properties;
    }

    public List<AlertResponse> list(AlertStatus status) {
        List<Alert> found = status == null ? alerts.findAll() : alerts.findByStatus(status);
        return found.stream().sorted(NEWEST_FIRST).map(AlertResponse::from).toList();
    }

    public List<AlertResponse> recent(int limit) {
        return alerts.findAll().stream().sorted(NEWEST_FIRST).limit(limit).map(AlertResponse::from).toList();
    }

    public int countOpen() {
        return alerts.findByStatus(AlertStatus.OPEN).size();
    }

    public AlertResponse resolve(Long id) {
        Alert alert = alerts.findById(id).orElseThrow(() -> new NotFoundException("Alert " + id + " was not found"));
        if (alert.getStatus() == AlertStatus.OPEN) {
            markResolved(alert);
        }
        return AlertResponse.from(alert);
    }

    /** Called after mortality is recorded. deathsOnDate is the TOTAL deaths for that flock on that date. */
    public void evaluateMortality(Flock flock, LocalDate date, int deathsOnDate) {
        BigDecimal threshold = properties.getAlerts().getDailyMortalityThresholdPercent();
        int initial = flock.getInitialBirdCount();
        if (!PerformanceCalculator.percentAtLeast(deathsOnDate, initial, threshold)) {
            return;
        }
        BigDecimal critical = threshold.multiply(properties.getAlerts().getCriticalMultiplier());
        AlertSeverity severity = PerformanceCalculator.percentAtLeast(deathsOnDate, initial, critical)
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        BigDecimal rate = PerformanceCalculator.dailyMortalityPercent(deathsOnDate, initial);
        String message = String.format(Locale.ROOT,
                "Flock %s: %d deaths recorded on %s = %s%% of the %d birds placed (alert threshold: %s%% per day). "
                        + "Prototype-generated notification.",
                flock.getFlockCode(), deathsOnDate, date, rate.toPlainString(), initial,
                threshold.stripTrailingZeros().toPlainString());
        upsertOpen(AlertType.HIGH_MORTALITY, severity, message, "FLOCK", flock.getId(),
                "HIGH_MORTALITY:" + flock.getId() + ":" + date);
    }

    /** Called after any change to a feed item's stock or reorder threshold. */
    public void evaluateLowStock(FeedItem item) {
        String key = "LOW_STOCK:" + item.getId();
        if (PerformanceCalculator.isLowStock(item.getQuantityOnHand(), item.getReorderThreshold())) {
            AlertSeverity severity = item.getQuantityOnHand().signum() == 0 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            String message = String.format(Locale.ROOT,
                    "Feed '%s' is at or below its reorder threshold: %s kg on hand (threshold %s kg). "
                            + "Prototype-generated notification.",
                    item.getName(), item.getQuantityOnHand().toPlainString(), item.getReorderThreshold().toPlainString());
            upsertOpen(AlertType.LOW_STOCK, severity, message, "FEED_ITEM", item.getId(), key);
        } else {
            alerts.findFirstByDedupKeyAndStatus(key, AlertStatus.OPEN).ifPresent(this::markResolved);
        }
    }

    private void upsertOpen(AlertType type, AlertSeverity severity, String message,
                            String entityType, Long entityId, String key) {
        Optional<Alert> open = alerts.findFirstByDedupKeyAndStatus(key, AlertStatus.OPEN);
        if (open.isPresent()) {
            Alert existing = open.get();
            existing.setSeverity(severity);
            existing.setMessage(message);
            alerts.save(existing);
        } else {
            alerts.save(new Alert(type, severity, message, entityType, entityId, key));
        }
    }

    private void markResolved(Alert alert) {
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        alerts.save(alert);
    }
}
