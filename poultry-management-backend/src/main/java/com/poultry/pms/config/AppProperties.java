package com.poultry.pms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * All prototype settings, bound from application.properties (keys start with "poultry.").
 * Keeping them in one class means no "mystery numbers" are scattered through the code.
 */
@ConfigurationProperties(prefix = "poultry")
public class AppProperties {

    private String timezone = "Africa/Nairobi";
    private final Alerts alerts = new Alerts();
    private final Cors cors = new Cors();
    private final DemoData demoData = new DemoData();

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Alerts getAlerts() { return alerts; }
    public Cors getCors() { return cors; }
    public DemoData getDemoData() { return demoData; }

    public static class Alerts {
        /** Percent of INITIAL birds that must die on one date to raise a high-mortality alert. */
        private BigDecimal dailyMortalityThresholdPercent = new BigDecimal("0.50");
        /** The alert is CRITICAL at threshold multiplied by this number. */
        private BigDecimal criticalMultiplier = new BigDecimal("2");

        public BigDecimal getDailyMortalityThresholdPercent() { return dailyMortalityThresholdPercent; }
        public void setDailyMortalityThresholdPercent(BigDecimal value) { this.dailyMortalityThresholdPercent = value; }
        public BigDecimal getCriticalMultiplier() { return criticalMultiplier; }
        public void setCriticalMultiplier(BigDecimal value) { this.criticalMultiplier = value; }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173", "http://127.0.0.1:5173"));

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class DemoData {
        private boolean enabled = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
