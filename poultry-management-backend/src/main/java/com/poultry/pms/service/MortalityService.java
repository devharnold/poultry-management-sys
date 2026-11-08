package com.poultry.pms.service;

import com.poultry.pms.config.TimeProvider;
import com.poultry.pms.dto.MortalityRequest;
import com.poultry.pms.dto.MortalityResponse;
import com.poultry.pms.entity.Flock;
import com.poultry.pms.entity.FlockStatus;
import com.poultry.pms.entity.MortalityRecord;
import com.poultry.pms.exception.BusinessRuleException;
import com.poultry.pms.exception.ConflictException;
import com.poultry.pms.repository.MortalityRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Mortality rules (one place):
 * 1. the flock must be ACTIVE
 * 2. the date cannot be in the future or before the flock's placement date
 * 3. the count must be positive and cannot exceed the birds currently alive
 * 4. the same clientOperationId is processed only once (safe offline retries)
 * Then the high-mortality alert rule is evaluated.
 */
@Service
@Transactional
public class MortalityService {

    private static final Comparator<MortalityRecord> NEWEST_FIRST =
            Comparator.comparing(MortalityRecord::getRecordDate).thenComparing(MortalityRecord::getId).reversed();

    private final MortalityRecordRepository mortality;
    private final FlockService flockService;
    private final AlertService alertService;
    private final TimeProvider time;

    public MortalityService(MortalityRecordRepository mortality, FlockService flockService,
                            AlertService alertService, TimeProvider time) {
        this.mortality = mortality;
        this.flockService = flockService;
        this.alertService = alertService;
        this.time = time;
    }

    public CreateResult<MortalityResponse> record(MortalityRequest request) {
        String operationId = FarmService.clean(request.clientOperationId());

        // Idempotency: a retry of an operation we already stored returns the original record.
        if (operationId != null) {
            Optional<MortalityRecord> existing = mortality.findByClientOperationId(operationId);
            if (existing.isPresent()) {
                MortalityRecord original = existing.get();
                if (!original.getFlock().getId().equals(request.flockId())) {
                    throw new ConflictException("clientOperationId", "This operation id was already used for a different record.");
                }
                return CreateResult.duplicate(MortalityResponse.from(original));
            }
        }

        Flock flock = flockService.find(request.flockId());
        if (flock.getStatus() != FlockStatus.ACTIVE) {
            throw new BusinessRuleException("flockId", "Flock " + flock.getFlockCode()
                    + " is not active, so new mortality cannot be recorded for it.");
        }
        LocalDate date = request.recordDate();
        if (date.isAfter(time.today())) {
            throw new BusinessRuleException("recordDate", "Date cannot be in the future.");
        }
        if (date.isBefore(flock.getPlacementDate())) {
            throw new BusinessRuleException("recordDate", "Date cannot be before the flock's placement date ("
                    + flock.getPlacementDate() + ").");
        }

        int count = request.count();
        int alive = flock.getInitialBirdCount() - flockService.totalMortality(flock.getId());
        if (count > alive) {
            throw new BusinessRuleException("count", "Cannot record " + count + " deaths: only " + alive
                    + " birds are currently alive in flock " + flock.getFlockCode() + ".");
        }

        int deathsAlreadyOnDate = mortality.findByFlockId(flock.getId()).stream()
                .filter(r -> r.getRecordDate().equals(date))
                .mapToInt(MortalityRecord::getDeathCount)
                .sum();

        MortalityRecord saved = mortality.save(
                new MortalityRecord(flock, date, count, FarmService.clean(request.reason()), operationId));
        alertService.evaluateMortality(flock, date, deathsAlreadyOnDate + count);
        return CreateResult.created(MortalityResponse.from(saved));
    }

    /** Optional filters: flock and an inclusive date range. Newest first. */
    public List<MortalityResponse> list(Long flockId, LocalDate from, LocalDate to) {
        return mortality.findAll().stream()
                .filter(r -> flockId == null || r.getFlock().getId().equals(flockId))
                .filter(r -> from == null || !r.getRecordDate().isBefore(from))
                .filter(r -> to == null || !r.getRecordDate().isAfter(to))
                .sorted(NEWEST_FIRST)
                .map(MortalityResponse::from)
                .toList();
    }

    public List<MortalityResponse> listForFlock(Long flockId) {
        flockService.find(flockId);   // 404 if the flock does not exist
        return list(flockId, null, null);
    }
}
