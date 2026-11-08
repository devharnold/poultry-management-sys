package com.poultry.pms.controller;

import com.poultry.pms.dto.FlockRequest;
import com.poultry.pms.dto.FlockResponse;
import com.poultry.pms.dto.MortalityResponse;
import com.poultry.pms.dto.PerformanceResponse;
import com.poultry.pms.dto.WeightRequest;
import com.poultry.pms.dto.WeightResponse;
import com.poultry.pms.entity.FlockStatus;
import com.poultry.pms.service.FlockService;
import com.poultry.pms.service.MortalityService;
import com.poultry.pms.service.PerformanceService;
import com.poultry.pms.service.WeightService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flocks")
public class FlockController {

    private final FlockService flockService;
    private final MortalityService mortalityService;
    private final WeightService weightService;
    private final PerformanceService performanceService;

    public FlockController(FlockService flockService, MortalityService mortalityService,
                           WeightService weightService, PerformanceService performanceService) {
        this.flockService = flockService;
        this.mortalityService = mortalityService;
        this.weightService = weightService;
        this.performanceService = performanceService;
    }

    @GetMapping
    public List<FlockResponse> list(@RequestParam(name = "farmId", required = false) Long farmId,
                                    @RequestParam(name = "status", required = false) FlockStatus status,
                                    @RequestParam(name = "q", required = false) String search) {
        return flockService.list(farmId, status, search);
    }

    @GetMapping("/{id}")
    public FlockResponse get(@PathVariable("id") Long id) {
        return flockService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlockResponse create(@Valid @RequestBody FlockRequest request) {
        return flockService.create(request);
    }

    @PutMapping("/{id}")
    public FlockResponse update(@PathVariable("id") Long id, @Valid @RequestBody FlockRequest request) {
        return flockService.update(id, request);
    }

    @GetMapping("/{id}/mortality")
    public List<MortalityResponse> mortality(@PathVariable("id") Long id) {
        return mortalityService.listForFlock(id);
    }

    @GetMapping("/{id}/weights")
    public List<WeightResponse> weights(@PathVariable("id") Long id) {
        return weightService.list(id);
    }

    @PostMapping("/{id}/weights")
    @ResponseStatus(HttpStatus.CREATED)
    public WeightResponse addWeight(@PathVariable("id") Long id, @Valid @RequestBody WeightRequest request) {
        return weightService.record(id, request);
    }

    @GetMapping("/{id}/performance")
    public PerformanceResponse performance(@PathVariable("id") Long id) {
        return performanceService.forFlock(id);
    }
}
