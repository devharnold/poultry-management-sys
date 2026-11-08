package com.poultry.pms.dto;

import java.math.BigDecimal;

/**
 * Flock performance indicators. mortalityRatePercent and fcr are null when they cannot be calculated honestly;
 * fcrStatus then explains why ("NOT_ENOUGH_DATA").
 */
public record PerformanceResponse(Long flockId, String flockCode, int initialBirdCount, int currentBirdCount,
                                  int totalMortality, BigDecimal mortalityRatePercent, String mortalityFormula,
                                  BigDecimal totalFeedUsedKg, int weightRecordCount, BigDecimal latestAverageWeightKg,
                                  BigDecimal fcr, String fcrStatus, String fcrMessage, String fcrFormula,
                                  String disclaimer) {
}
