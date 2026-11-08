package com.poultry.pms.controller;

import com.poultry.pms.dto.DemoSeedResponse;
import com.poultry.pms.service.DemoDataService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoDataService demoDataService;

    public DemoController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("demoDataLoaded", demoDataService.isLoaded());
    }

    /** Loads synthetic sample data once. A second call answers 409 Conflict and changes nothing. */
    @PostMapping("/seed")
    @ResponseStatus(HttpStatus.CREATED)
    public DemoSeedResponse seed() {
        return demoDataService.seed();
    }
}
