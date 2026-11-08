package com.poultry.pms.service;

import com.poultry.pms.config.TimeProvider;
import com.poultry.pms.dto.DemoSeedResponse;
import com.poultry.pms.dto.FarmRequest;
import com.poultry.pms.dto.FarmResponse;
import com.poultry.pms.dto.FeedItemRequest;
import com.poultry.pms.dto.FeedItemResponse;
import com.poultry.pms.dto.FlockRequest;
import com.poultry.pms.dto.FlockResponse;
import com.poultry.pms.dto.MortalityRequest;
import com.poultry.pms.dto.UsageRequest;
import com.poultry.pms.dto.WeightRequest;
import com.poultry.pms.entity.FlockStatus;
import com.poultry.pms.exception.ConflictException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Loads CLEARLY SYNTHETIC sample records so the screens have something to show.
 * It goes through the normal services, so every business rule and alert applies to demo data too.
 * A marker row in app_meta guarantees it runs only once (restarting the app never duplicates it).
 * All names say "DEMO" and no real Kenchic or farmer data is used.
 */
@Service
@Transactional
public class DemoDataService {

    static final String META_KEY = "demo_data_seeded_at";

    private final JdbcTemplate jdbc;
    private final FarmService farmService;
    private final FlockService flockService;
    private final MortalityService mortalityService;
    private final FeedService feedService;
    private final WeightService weightService;
    private final TimeProvider time;

    public DemoDataService(JdbcTemplate jdbc, FarmService farmService, FlockService flockService,
                           MortalityService mortalityService, FeedService feedService,
                           WeightService weightService, TimeProvider time) {
        this.jdbc = jdbc;
        this.farmService = farmService;
        this.flockService = flockService;
        this.mortalityService = mortalityService;
        this.feedService = feedService;
        this.weightService = weightService;
        this.time = time;
    }

    public boolean isLoaded() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM app_meta WHERE meta_key = ?", Integer.class, META_KEY);
        return count != null && count > 0;
    }

    public DemoSeedResponse seed() {
        if (isLoaded()) {
            throw new ConflictException(null, "Demo data was already loaded earlier, so nothing was added. "
                    + "To start again, stop the backend and delete the data/poultry.db file (see README).");
        }
        LocalDate today = time.today();

        FarmResponse alpha = farmService.create(new FarmRequest("DEMO Farm Alpha (synthetic)",
                "Demo location A - not a real site", "Synthetic demo record"));
        FarmResponse beta = farmService.create(new FarmRequest("DEMO Farm Beta (synthetic)",
                "Demo location B - not a real site", "Synthetic demo record"));

        FlockResponse f1 = flockService.create(new FlockRequest("DEMO-001", alpha.id(), "Broiler (demo)",
                today.minusDays(28), 2000, FlockStatus.ACTIVE, "Synthetic demo flock"));
        FlockResponse f2 = flockService.create(new FlockRequest("DEMO-002", alpha.id(), "Layer (demo)",
                today.minusDays(10), 1500, FlockStatus.ACTIVE, "Synthetic demo flock"));
        FlockResponse f3 = flockService.create(new FlockRequest("DEMO-003", beta.id(), "Broiler (demo)",
                today.minusDays(14), 3000, FlockStatus.ACTIVE, "Synthetic demo flock"));

        int records = 0;
        records += death(f1.id(), today.minusDays(5), 3, "Demo: routine");
        records += death(f1.id(), today.minusDays(4), 2, "Demo: routine");
        records += death(f1.id(), today.minusDays(3), 4, "Demo: routine");
        records += death(f1.id(), today.minusDays(2), 3, "Demo: routine");
        records += death(f1.id(), today.minusDays(1), 14, "Demo: unusual day (raises a prototype alert)");
        records += death(f1.id(), today, 4, "Demo: routine");
        records += death(f2.id(), today.minusDays(2), 2, "Demo: routine");
        records += death(f2.id(), today.minusDays(1), 3, "Demo: routine");
        records += death(f3.id(), today.minusDays(1), 5, "Demo: routine");

        FeedItemResponse broiler = feedService.create(new FeedItemRequest("DEMO Broiler Feed", "Broiler grower",
                new BigDecimal("1000"), new BigDecimal("5000"), "Demo supplier", "Synthetic demo stock"),
                today.minusDays(28));
        FeedItemResponse layer = feedService.create(new FeedItemRequest("DEMO Layer Mash", "Layer mash",
                new BigDecimal("300"), new BigDecimal("900"), "Demo supplier", "Synthetic demo stock"),
                today.minusDays(10));
        feedService.create(new FeedItemRequest("DEMO Starter Crumble", "Starter crumble",
                new BigDecimal("100"), new BigDecimal("400"), "Demo supplier", "Synthetic demo stock"), today);

        usage(broiler.id(), f1.id(), today.minusDays(21), "600");
        usage(broiler.id(), f1.id(), today.minusDays(14), "1000");
        usage(broiler.id(), f1.id(), today.minusDays(7), "1200");
        usage(broiler.id(), f1.id(), today.minusDays(1), "1400");   // leaves 800 kg: below the 1000 kg threshold
        usage(layer.id(), f2.id(), today.minusDays(2), "60");
        usage(layer.id(), f2.id(), today.minusDays(1), "65");

        weightService.record(f1.id(), new WeightRequest(today.minusDays(28), new BigDecimal("0.045"), 50, "Demo: chick weight at placement"));
        weightService.record(f1.id(), new WeightRequest(today.minusDays(1), new BigDecimal("1.350"), 50, "Demo: sample weighing"));

        jdbc.update("INSERT INTO app_meta (meta_key, meta_value) VALUES (?, ?)", META_KEY, Instant.now().toString());
        return new DemoSeedResponse(true, "Synthetic demo data loaded. Everything named DEMO is made up.", 2, 3, records, 3);
    }

    private int death(Long flockId, LocalDate date, int count, String reason) {
        mortalityService.record(new MortalityRequest(flockId, date, count, reason, null));
        return 1;
    }

    private void usage(Long itemId, Long flockId, LocalDate date, String kg) {
        feedService.recordUsage(itemId, new UsageRequest(new BigDecimal(kg), date, flockId, "Demo usage", null));
    }
}
