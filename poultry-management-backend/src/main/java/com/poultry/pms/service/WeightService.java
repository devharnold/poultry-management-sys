package com.poultry.pms.service;

import com.poultry.pms.config.TimeProvider;
import com.poultry.pms.dto.WeightRequest;
import com.poultry.pms.dto.WeightResponse;
import com.poultry.pms.entity.Flock;
import com.poultry.pms.entity.WeightRecord;
import com.poultry.pms.exception.BusinessRuleException;
import com.poultry.pms.exception.ConflictException;
import com.poultry.pms.repository.WeightRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Average bird weight samples. They are only ever entered by a person: nothing is estimated or invented. */
@Service
@Transactional
public class WeightService {

    private final WeightRecordRepository weights;
    private final FlockService flockService;
    private final TimeProvider time;

    public WeightService(WeightRecordRepository weights, FlockService flockService, TimeProvider time) {
        this.weights = weights;
        this.flockService = flockService;
        this.time = time;
    }

    public WeightResponse record(Long flockId, WeightRequest request) {
        Flock flock = flockService.find(flockId);
        if (request.recordDate().isAfter(time.today())) {
            throw new BusinessRuleException("recordDate", "Date cannot be in the future.");
        }
        if (request.recordDate().isBefore(flock.getPlacementDate())) {
            throw new BusinessRuleException("recordDate", "Date cannot be before the flock's placement date ("
                    + flock.getPlacementDate() + ").");
        }
        if (weights.existsByFlockIdAndRecordDate(flockId, request.recordDate())) {
            throw new ConflictException("recordDate", "A weight is already recorded for this flock on that date.");
        }
        WeightRecord saved = weights.save(new WeightRecord(flock, request.recordDate(),
                request.averageBirdWeightKg(), request.sampledBirdCount(), FarmService.clean(request.notes())));
        return WeightResponse.from(saved);
    }

    public List<WeightResponse> list(Long flockId) {
        flockService.find(flockId);
        return weights.findByFlockId(flockId).stream()
                .sorted(Comparator.comparing(WeightRecord::getRecordDate))
                .map(WeightResponse::from)
                .toList();
    }
}
