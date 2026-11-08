package com.poultry.pms.service;

import com.poultry.pms.config.TimeProvider;
import com.poultry.pms.dto.FlockRequest;
import com.poultry.pms.dto.FlockResponse;
import com.poultry.pms.entity.Farm;
import com.poultry.pms.entity.Flock;
import com.poultry.pms.entity.FlockStatus;
import com.poultry.pms.entity.MortalityRecord;
import com.poultry.pms.exception.BusinessRuleException;
import com.poultry.pms.exception.ConflictException;
import com.poultry.pms.exception.NotFoundException;
import com.poultry.pms.repository.FarmRepository;
import com.poultry.pms.repository.FlockRepository;
import com.poultry.pms.repository.MortalityRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Flock rules live here (and only here):
 * - flock codes are unique (case-insensitive) and stored in UPPER CASE
 * - placement date cannot be in the future
 * - current birds = initial birds - total recorded deaths (derived every time, never stored)
 */
@Service
@Transactional
public class FlockService {

    private final FlockRepository flocks;
    private final FarmRepository farms;
    private final MortalityRecordRepository mortality;
    private final TimeProvider time;

    public FlockService(FlockRepository flocks, FarmRepository farms, MortalityRecordRepository mortality, TimeProvider time) {
        this.flocks = flocks;
        this.farms = farms;
        this.mortality = mortality;
        this.time = time;
    }

    /** All filters are optional. search matches code, bird type or farm name. */
    public List<FlockResponse> list(Long farmId, FlockStatus status, String search) {
        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Map<Long, Integer> deathsByFlock = new HashMap<>();
        for (MortalityRecord record : mortality.findAll()) {
            deathsByFlock.merge(record.getFlock().getId(), record.getDeathCount(), Integer::sum);
        }
        return flocks.findAll().stream()
                .filter(f -> farmId == null || f.getFarm().getId().equals(farmId))
                .filter(f -> status == null || f.getStatus() == status)
                .filter(f -> needle.isEmpty()
                        || f.getFlockCode().toLowerCase(Locale.ROOT).contains(needle)
                        || f.getBirdType().toLowerCase(Locale.ROOT).contains(needle)
                        || f.getFarm().getName().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(Flock::getPlacementDate).thenComparing(Flock::getId).reversed())
                .map(f -> FlockResponse.from(f, deathsByFlock.getOrDefault(f.getId(), 0)))
                .toList();
    }

    public FlockResponse get(Long id) {
        Flock flock = find(id);
        return FlockResponse.from(flock, totalMortality(id));
    }

    public FlockResponse create(FlockRequest request) {
        String code = normalizeCode(request.flockCode());
        if (flocks.findByFlockCodeIgnoreCase(code).isPresent()) {
            throw new ConflictException("flockCode", "A flock with code " + code + " already exists.");
        }
        Farm farm = findFarm(request.farmId());
        checkPlacementDate(request.placementDate());
        FlockStatus status = request.status() == null ? FlockStatus.ACTIVE : request.status();
        Flock flock = flocks.save(new Flock(code, farm, request.birdType().trim(), request.placementDate(),
                request.initialBirdCount(), status, FarmService.clean(request.notes())));
        return FlockResponse.from(flock, 0);
    }

    public FlockResponse update(Long id, FlockRequest request) {
        Flock flock = find(id);
        String code = normalizeCode(request.flockCode());
        Optional<Flock> sameCode = flocks.findByFlockCodeIgnoreCase(code);
        if (sameCode.isPresent() && !sameCode.get().getId().equals(id)) {
            throw new ConflictException("flockCode", "Another flock already uses code " + code + ".");
        }
        Farm farm = findFarm(request.farmId());
        checkPlacementDate(request.placementDate());

        // Editing must never make the history impossible: deaths already recorded must still fit.
        List<MortalityRecord> deaths = mortality.findByFlockId(id);
        int totalDeaths = deaths.stream().mapToInt(MortalityRecord::getDeathCount).sum();
        if (request.initialBirdCount() < totalDeaths) {
            throw new BusinessRuleException("initialBirdCount", "Initial bird count (" + request.initialBirdCount()
                    + ") cannot be lower than the " + totalDeaths + " deaths already recorded for this flock.");
        }
        Optional<LocalDate> earliestDeath = deaths.stream().map(MortalityRecord::getRecordDate).min(Comparator.naturalOrder());
        if (earliestDeath.isPresent() && request.placementDate().isAfter(earliestDeath.get())) {
            throw new BusinessRuleException("placementDate", "Placement date cannot be after the first recorded mortality ("
                    + earliestDeath.get() + ").");
        }

        flock.setFlockCode(code);
        flock.setFarm(farm);
        flock.setBirdType(request.birdType().trim());
        flock.setPlacementDate(request.placementDate());
        flock.setInitialBirdCount(request.initialBirdCount());
        if (request.status() != null) {
            flock.setStatus(request.status());
        }
        flock.setNotes(FarmService.clean(request.notes()));
        return FlockResponse.from(flocks.saveAndFlush(flock), totalDeaths);
    }

    public Flock find(Long id) {
        return flocks.findById(id).orElseThrow(() -> new NotFoundException("Flock " + id + " was not found."));
    }

    /** The single place that answers "how many birds have died in this flock so far?". */
    public int totalMortality(Long flockId) {
        return mortality.findByFlockId(flockId).stream().mapToInt(MortalityRecord::getDeathCount).sum();
    }

    private Farm findFarm(Long farmId) {
        return farms.findById(farmId).orElseThrow(() -> new BusinessRuleException("farmId", "The selected farm does not exist."));
    }

    private void checkPlacementDate(LocalDate placementDate) {
        if (placementDate.isAfter(time.today())) {
            throw new BusinessRuleException("placementDate", "Placement date cannot be in the future.");
        }
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
