package com.poultry.pms.config;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Decides what "today" means. Dates the user sees use the configured time zone (default Africa/Nairobi);
 * technical timestamps (createdAt/updatedAt) are always stored in UTC.
 */
@Component
public class TimeProvider {

    private final ZoneId zone;

    public TimeProvider(AppProperties properties) {
        this.zone = ZoneId.of(properties.getTimezone());
    }

    public LocalDate today() {
        return LocalDate.now(zone);
    }

    public ZoneId zone() {
        return zone;
    }
}
