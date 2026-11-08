package com.poultry.pms.service;

import com.poultry.pms.dto.PerformanceResponse;
import com.poultry.pms.entity.FeedMovement;
import com.poultry.pms.entity.Flock;
import com.poultry.pms.entity.MortalityRecord;
import com.poultry.pms.entity.MovementType;
import com.poultry.pms.entity.WeightRecord;
import com.poultry.pms.repository.FeedMovementRepository;
import com.poultry.pms.repository.MortalityRecordRepository;
import com.poultry.pms.repository.WeightRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Gathers the recorded data for one flock and hands it to {@link PerformanceCalculator}.
 * Missing data is reported as "NOT_ENOUGH_DATA"; nothing is ever estimated.
 */
@Service
@Transactional
public class PerformanceService {

    public static final String MORTALITY_FORMULA = "mortality rate % = total recorded deaths / initial birds placed x 100";
    public static final String FCR_FORMULA =
            "FCR = feed used by the flock between the first and last weighing (kg) / live-weight gain (kg); "
                    + "live weight = average bird weight x birds alive on that date";
    public static final String DISCLAIMER =
            "Educational prototype: these indicators have not been validated and must be checked with qualified "
                    + "poultry professionals before any operational use.";

    private final FlockService flockService;
    private final MortalityRecordRepository mortality;
    private final FeedMovementRepository movements;
    private final WeightRecordRepository weights;

    public PerformanceService(FlockService flockService, MortalityRecordRepository mortality,
                              FeedMovementRepository movements, WeightRecordRepository weights) {
        this.flockService = flockService;
        this.mortality = mortality;
        this.movements = movements;
        this.weights = weights;
    }

    public PerformanceResponse forFlock(Long flockId) {
        Flock flock = flockService.find(flockId);
        List<MortalityRecord> deaths = mortality.findByFlockId(flockId);
        int initial = flock.getInitialBirdCount();
        int totalDeaths = deaths.stream().mapToInt(MortalityRecord::getDeathCount).sum();

        List<FeedMovement> usage = movements.findByFlockId(flockId).stream()
                .filter(m -> m.getMovementType() == MovementType.USAGE)
                .toList();
        BigDecimal totalFeed = sumQuantity(usage, null, null);

        List<WeightRecord> weighings = weights.findByFlockId(flockId).stream()
                .sorted(Comparator.comparing(WeightRecord::getRecordDate))
                .toList();
        BigDecimal latestWeight = weighings.isEmpty() ? null : weighings.get(weighings.size() - 1).getAverageBirdWeightKg();

        BigDecimal fcr = null;
        String status = "NOT_ENOUGH_DATA";
        String message;
        if (weighings.size() < 2) {
            message = "Not enough data yet: record at least two average-weight measurements "
                    + "(ideally one on the placement date) to calculate feed conversion.";
        } else {
            WeightRecord first = weighings.get(0);
            WeightRecord last = weighings.get(weighings.size() - 1);
            int aliveFirst = initial - deathsUpTo(deaths, first.getRecordDate());
            int aliveLast = initial - deathsUpTo(deaths, last.getRecordDate());
            BigDecimal gain = PerformanceCalculator.liveWeightKg(last.getAverageBirdWeightKg(), aliveLast)
                    .subtract(PerformanceCalculator.liveWeightKg(first.getAverageBirdWeightKg(), aliveFirst));
            BigDecimal feedInWindow = sumQuantity(usage, first.getRecordDate(), last.getRecordDate());
            fcr = PerformanceCalculator.fcr(feedInWindow, gain);
            if (fcr == null) {
                message = "Not enough data yet: this needs feed usage recorded for this flock between the first and last "
                        + "weighing and a positive live-weight gain.";
            } else {
                status = "OK";
                message = String.format(Locale.ROOT, "%s kg feed / %s kg live-weight gain between %s and %s.",
                        feedInWindow.toPlainString(), gain.toPlainString(), first.getRecordDate(), last.getRecordDate());
            }
        }

        return new PerformanceResponse(flock.getId(), flock.getFlockCode(), initial, initial - totalDeaths, totalDeaths,
                PerformanceCalculator.mortalityRatePercent(totalDeaths, initial), MORTALITY_FORMULA,
                totalFeed, weighings.size(), latestWeight, fcr, status, message, FCR_FORMULA, DISCLAIMER);
    }

    private static int deathsUpTo(List<MortalityRecord> deaths, LocalDate date) {
        return deaths.stream().filter(d -> !d.getRecordDate().isAfter(date)).mapToInt(MortalityRecord::getDeathCount).sum();
    }

    /** Sum usage with date > after (when given) and date <= until (when given). */
    private static BigDecimal sumQuantity(List<FeedMovement> usage, LocalDate after, LocalDate until) {
        BigDecimal total = BigDecimal.ZERO;
        for (FeedMovement m : usage) {
            boolean afterOk = after == null || m.getMovementDate().isAfter(after);
            boolean untilOk = until == null || !m.getMovementDate().isAfter(until);
            if (afterOk && untilOk) {
                total = total.add(m.getQuantity());
            }
        }
        return total.setScale(3, java.math.RoundingMode.HALF_UP);
    }
}
