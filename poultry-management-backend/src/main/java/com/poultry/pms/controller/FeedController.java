package com.poultry.pms.controller;

import com.poultry.pms.dto.FeedItemRequest;
import com.poultry.pms.dto.FeedItemResponse;
import com.poultry.pms.dto.FeedMovementResponse;
import com.poultry.pms.dto.StockInRequest;
import com.poultry.pms.dto.UsageRequest;
import com.poultry.pms.service.CreateResult;
import com.poultry.pms.service.FeedService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feed-items")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public List<FeedItemResponse> list() {
        return feedService.list();
    }

    @GetMapping("/{id}")
    public FeedItemResponse get(@PathVariable("id") Long id) {
        return feedService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedItemResponse create(@Valid @RequestBody FeedItemRequest request) {
        return feedService.create(request);
    }

    @PutMapping("/{id}")
    public FeedItemResponse update(@PathVariable("id") Long id, @Valid @RequestBody FeedItemRequest request) {
        return feedService.update(id, request);
    }

    @PostMapping("/{id}/stock-in")
    public ResponseEntity<FeedMovementResponse> stockIn(@PathVariable("id") Long id,
                                                        @Valid @RequestBody StockInRequest request) {
        return toResponse(feedService.stockIn(id, request));
    }

    @PostMapping("/{id}/usage")
    public ResponseEntity<FeedMovementResponse> usage(@PathVariable("id") Long id,
                                                      @Valid @RequestBody UsageRequest request) {
        return toResponse(feedService.recordUsage(id, request));
    }

    @GetMapping("/{id}/movements")
    public List<FeedMovementResponse> movements(@PathVariable("id") Long id) {
        return feedService.movements(id);
    }

    private ResponseEntity<FeedMovementResponse> toResponse(CreateResult<FeedMovementResponse> result) {
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.body());
    }
}
