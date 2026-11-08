package com.poultry.pms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Every number here is calculated from the records currently stored in the database. */
public record DashboardResponse(LocalDate today, String timezone, int farmCount, int totalActiveFlocks, int totalBirds,
                                int mortalityToday, int lowStockCount, List<LowStockItem> lowStockItems,
                                int openAlertCount, List<AlertResponse> recentAlerts, boolean demoDataLoaded,
                                String dataNote) {

    public record LowStockItem(Long id, String name, BigDecimal quantityOnHand, BigDecimal reorderThreshold, String unit) { }
}
