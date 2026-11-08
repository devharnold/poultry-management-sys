package com.poultry.pms.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain unit tests: no Spring, no database. They document every formula used by the app. */
class PerformanceCalculatorTest {

    @Test
    void percentIsRoundedToTwoDecimals() {
        assertEquals(new BigDecimal("0.70"), PerformanceCalculator.percent(14, 2000));
        assertEquals(new BigDecimal("33.33"), PerformanceCalculator.percent(1, 3));
    }

    @Test
    void percentIsNullWhenTheWholeIsNotPositive() {
        assertNull(PerformanceCalculator.percent(5, 0));
        assertNull(PerformanceCalculator.mortalityRatePercent(5, -1));
    }

    @Test
    void thresholdTestIsExactAtTheBoundary() {
        BigDecimal threshold = new BigDecimal("0.50");
        // 10 of 2000 birds is exactly 0.50 %  -> alert
        assertTrue(PerformanceCalculator.percentAtLeast(10, 2000, threshold));
        // 9 of 2000 birds is 0.45 % -> no alert
        assertFalse(PerformanceCalculator.percentAtLeast(9, 2000, threshold));
        // 499 of 100000 is 0.499 %: must NOT round up into an alert
        assertFalse(PerformanceCalculator.percentAtLeast(499, 100000, threshold));
    }

    @Test
    void thresholdTestIsFalseWithoutBirds() {
        assertFalse(PerformanceCalculator.percentAtLeast(1, 0, new BigDecimal("0.5")));
    }

    @Test
    void liveWeightIsAverageWeightTimesBirdsAlive() {
        assertEquals(new BigDecimal("2664.900"), PerformanceCalculator.liveWeightKg(new BigDecimal("1.350"), 1974));
    }

    @Test
    void fcrIsFeedDividedByWeightGain() {
        // the demo-data numbers: 4200 kg feed, (1.350 x 1974) - (0.045 x 2000) = 2574.900 kg gain
        BigDecimal gain = PerformanceCalculator.liveWeightKg(new BigDecimal("1.350"), 1974)
                .subtract(PerformanceCalculator.liveWeightKg(new BigDecimal("0.045"), 2000));
        assertEquals(new BigDecimal("2574.900"), gain);
        assertEquals(new BigDecimal("1.63"), PerformanceCalculator.fcr(new BigDecimal("4200"), gain));
    }

    @Test
    void fcrIsNullInsteadOfAMisleadingNumber() {
        assertNull(PerformanceCalculator.fcr(BigDecimal.ZERO, new BigDecimal("10")));
        assertNull(PerformanceCalculator.fcr(new BigDecimal("10"), BigDecimal.ZERO));
        assertNull(PerformanceCalculator.fcr(new BigDecimal("10"), new BigDecimal("-5")));
        assertNull(PerformanceCalculator.fcr(null, new BigDecimal("5")));
    }

    @Test
    void lowStockMeansAtOrBelowTheThreshold() {
        BigDecimal threshold = new BigDecimal("1000.000");
        assertTrue(PerformanceCalculator.isLowStock(new BigDecimal("800.000"), threshold));
        assertTrue(PerformanceCalculator.isLowStock(new BigDecimal("1000.000"), threshold));
        assertFalse(PerformanceCalculator.isLowStock(new BigDecimal("1000.001"), threshold));
    }

    @Test
    void quantitiesAlwaysUseThreeDecimals() {
        assertEquals(new BigDecimal("12.500"), PerformanceCalculator.quantity(new BigDecimal("12.5")));
        assertEquals(new BigDecimal("0.333"), PerformanceCalculator.quantity(new BigDecimal("0.3333")));
    }
}
