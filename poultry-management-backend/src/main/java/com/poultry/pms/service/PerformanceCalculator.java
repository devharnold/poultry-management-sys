package com.poultry.pms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Every formula in the app lives here, as plain static methods with no database or Spring involved.
 * That makes them easy to read and easy to unit test.
 *
 * <pre>
 *  mortality rate %        = total deaths          / initial birds placed * 100
 *  daily mortality %       = deaths on one date    / initial birds placed * 100
 *  live weight (kg)        = average bird weight (kg) * birds alive at that date
 *  live-weight gain (kg)   = live weight at last weighing - live weight at first weighing
 *  feed conversion ratio   = feed used between the two weighings (kg) / live-weight gain (kg)
 *  low stock               = quantity on hand <= reorder threshold
 * </pre>
 *
 * Educational prototype: these indicators must be validated with qualified poultry professionals before any operational use.
 */
public final class PerformanceCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private PerformanceCalculator() { }

    /** part / whole * 100 rounded to 2 decimals, or null when the whole is not positive. */
    public static BigDecimal percent(long part, long whole) {
        if (whole <= 0) {
            return null;
        }
        return BigDecimal.valueOf(part).multiply(HUNDRED).divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal mortalityRatePercent(int totalDeaths, int initialBirds) {
        return percent(totalDeaths, initialBirds);
    }

    public static BigDecimal dailyMortalityPercent(int deathsOnDate, int initialBirds) {
        return percent(deathsOnDate, initialBirds);
    }

    /**
     * Exact threshold test (no rounding): part / whole * 100 >= thresholdPercent,
     * rewritten as part * 100 >= thresholdPercent * whole so tiny rounding errors cannot flip the result.
     */
    public static boolean percentAtLeast(long part, long whole, BigDecimal thresholdPercent) {
        if (whole <= 0 || thresholdPercent == null) {
            return false;
        }
        BigDecimal left = BigDecimal.valueOf(part).multiply(HUNDRED);
        BigDecimal right = thresholdPercent.multiply(BigDecimal.valueOf(whole));
        return left.compareTo(right) >= 0;
    }

    /** Average weight (kg per bird) times birds alive, 3 decimals. */
    public static BigDecimal liveWeightKg(BigDecimal averageBirdWeightKg, int aliveBirds) {
        if (averageBirdWeightKg == null || aliveBirds < 0) {
            return null;
        }
        return averageBirdWeightKg.multiply(BigDecimal.valueOf(aliveBirds)).setScale(3, RoundingMode.HALF_UP);
    }

    /** Feed conversion ratio, or null when feed or gain is missing/not positive (never a misleading number). */
    public static BigDecimal fcr(BigDecimal feedKg, BigDecimal liveWeightGainKg) {
        if (feedKg == null || liveWeightGainKg == null || feedKg.signum() <= 0 || liveWeightGainKg.signum() <= 0) {
            return null;
        }
        return feedKg.divide(liveWeightGainKg, 2, RoundingMode.HALF_UP);
    }

    /** Stock is "low" when it is at or below the reorder threshold. */
    public static boolean isLowStock(BigDecimal quantityOnHand, BigDecimal reorderThreshold) {
        return quantityOnHand.compareTo(reorderThreshold) <= 0;
    }

    /** All feed quantities use exactly 3 decimals (grams precision). */
    public static BigDecimal quantity(BigDecimal value) {
        return value == null ? null : value.setScale(3, RoundingMode.HALF_UP);
    }
}
