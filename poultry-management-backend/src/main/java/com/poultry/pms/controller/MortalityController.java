package com.poultry.pms.controller;

import com.poultry.pms.dto.MortalityRequest;
import com.poultry.pms.dto.MortalityResponse;
import com.poultry.pms.service.CreateResult;
import com.poultry.pms.service.MortalityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/mortality")
public class MortalityController {

    private final MortalityService mortalityService;

    public MortalityController(MortalityService mortalityService) {
        this.mortalityService = mortalityService;
    }

    @GetMapping
    public List<MortalityResponse> list(
            @RequestParam(name = "flockId", required = false) Long flockId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return mortalityService.list(flockId, from, to);
    }

    /** 201 Created for a new record; 200 OK when the same clientOperationId was already processed (safe retry). */
    @PostMapping
    public ResponseEntity<MortalityResponse> record(@Valid @RequestBody MortalityRequest request) {
        CreateResult<MortalityResponse> result = mortalityService.record(request);
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.body());
    }
}
