package com.poultry.pms.service;

import com.poultry.pms.config.TimeProvider;
import com.poultry.pms.dto.AlertResponse;
import com.poultry.pms.dto.DashboardResponse;
import com.poultry.pms.dto.DashboardResponse.LowStockItem;
import com.poultry.pms.entity.FeedItem;
import com.poultry.pms.entity.Flock;
import com.poultry.pms.entity.FlockStatus;
import com.poultry.pms.entity.MortalityRecord;
import com.poultry.pms.repository.FarmRepository;
import com.poultry.pms.repository.FeedItemRepository;
import com.poultry.pms.repository.FlockRepository;
import com.poultry.pms.repository.MortalityRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Every dashboard number is computed from stored records on each request: nothing is hard-coded or cached. */
@Service
@Transactional
public class DashboardService {

    private final FarmRepository farms;
    private final FlockRepository flocks;
    private final MortalityRecordRepository mortality;
    private final FeedItemRepository feedItems;
    private final AlertService alertService;
    private final DemoDataService demoDataService;
    private final TimeProvider time;

    public DashboardService(FarmRepository farms, FlockRepository flocks, MortalityRecordRepository mortality,
                            FeedItemRepository feedItems, AlertService alertService,
                            DemoDataService demoDataService, TimeProvider time) {
        this.farms = farms;
        this.flocks = flocks;
        this.mortality = mortality;
        this.feedItems = feedItems;
        this.alertService = alertService;
        this.demoDataService = demoDataService;
        this.time = time;
    }

    public DashboardResponse summary() {
        LocalDate today = time.today();

        java.util.Map<Long, Integer> deathsByFlock = new java.util.HashMap<>();
        for (MortalityRecord record : mortality.findAll()) {
            deathsByFlock.merge(record.getFlock().getId(), record.getDeathCount(), Integer::sum);
        }

        int activeFlocks = 0;
        int totalBirds = 0;
        for (Flock flock : flocks.findAll()) {
            if (flock.getStatus() == FlockStatus.ACTIVE) {
                activeFlocks++;
                totalBirds += flock.getInitialBirdCount() - deathsByFlock.getOrDefault(flock.getId(), 0);
            }
        }

        int mortalityToday = mortality.findByRecordDate(today).stream().mapToInt(MortalityRecord::getDeathCount).sum();

        List<LowStockItem> lowStock = feedItems.findAll().stream()
                .filter(i -> PerformanceCalculator.isLowStock(i.getQuantityOnHand(), i.getReorderThreshold()))
                .sorted(Comparator.comparing((FeedItem i) -> i.getName().toLowerCase(Locale.ROOT)))
                .map(i -> new LowStockItem(i.getId(), i.getName(), i.getQuantityOnHand(), i.getReorderThreshold(), i.getUnit()))
                .toList();

        List<AlertResponse> recentAlerts = alertService.recent(5);
        boolean demo = demoDataService.isLoaded();
        String note = demo
                ? "Calculated from the records currently in the local database. Synthetic DEMO records are included."
                : "Calculated from the records currently in the local database.";

        return new DashboardResponse(today, time.zone().getId(), (int) farms.count(), activeFlocks, totalBirds,
                mortalityToday, lowStock.size(), lowStock, alertService.countOpen(), recentAlerts, demo, note);
    }
}
