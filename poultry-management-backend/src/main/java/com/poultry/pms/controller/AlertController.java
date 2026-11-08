package com.poultry.pms.controller;

import com.poultry.pms.dto.AlertResponse;
import com.poultry.pms.entity.AlertStatus;
import com.poultry.pms.service.AlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Alerts are created by the backend rules. This API can only list them and mark them resolved. */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertResponse> list(@RequestParam(name = "status", required = false) AlertStatus status) {
        return alertService.list(status);
    }

    @PostMapping("/{id}/resolve")
    public AlertResponse resolve(@PathVariable("id") Long id) {
        return alertService.resolve(id);
    }
}
