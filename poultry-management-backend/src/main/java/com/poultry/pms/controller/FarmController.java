package com.poultry.pms.controller;

import com.poultry.pms.dto.FarmRequest;
import com.poultry.pms.dto.FarmResponse;
import com.poultry.pms.service.FarmService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** Controllers only translate HTTP to service calls. Business rules live in the service classes. */
@RestController
@RequestMapping("/api/farms")
public class FarmController {

    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    @GetMapping
    public List<FarmResponse> list(@RequestParam(name = "q", required = false) String search) {
        return farmService.list(search);
    }

    @GetMapping("/{id}")
    public FarmResponse get(@PathVariable("id") Long id) {
        return farmService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FarmResponse create(@Valid @RequestBody FarmRequest request) {
        return farmService.create(request);
    }

    @PutMapping("/{id}")
    public FarmResponse update(@PathVariable("id") Long id, @Valid @RequestBody FarmRequest request) {
        return farmService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        farmService.delete(id);
    }
}
