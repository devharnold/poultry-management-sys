package com.poultry.pms.service;

import com.poultry.pms.config.TimeProvider;
import com.poultry.pms.dto.FeedItemRequest;
import com.poultry.pms.dto.FeedItemResponse;
import com.poultry.pms.dto.FeedMovementResponse;
import com.poultry.pms.dto.StockInRequest;
import com.poultry.pms.dto.UsageRequest;
import com.poultry.pms.entity.FeedItem;
import com.poultry.pms.entity.FeedMovement;
import com.poultry.pms.entity.Flock;
import com.poultry.pms.entity.MovementType;
import com.poultry.pms.exception.BusinessRuleException;
import com.poultry.pms.exception.ConflictException;
import com.poultry.pms.exception.NotFoundException;
import com.poultry.pms.repository.FeedItemRepository;
import com.poultry.pms.repository.FeedMovementRepository;
import com.poultry.pms.repository.FlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Feed inventory rules (one place). Everything is in kilograms.
 * - stock changes ONLY through movements: STOCK_IN adds, USAGE subtracts
 * - usage can never take stock below zero
 * - every movement stores the balance after it, so history explains the current quantity
 * - the same clientOperationId is applied only once (safe offline retries)
 * - the low-stock alert rule is re-evaluated after every change
 */
@Service
@Transactional
public class FeedService {

    private static final Comparator<FeedMovement> NEWEST_FIRST =
            Comparator.comparing(FeedMovement::getMovementDate).thenComparing(FeedMovement::getId).reversed();

    private final FeedItemRepository items;
    private final FeedMovementRepository movements;
    private final FlockRepository flocks;
    private final AlertService alertService;
    private final TimeProvider time;

    public FeedService(FeedItemRepository items, FeedMovementRepository movements, FlockRepository flocks,
                       AlertService alertService, TimeProvider time) {
        this.items = items;
        this.movements = movements;
        this.flocks = flocks;
        this.alertService = alertService;
        this.time = time;
    }

    public List<FeedItemResponse> list() {
        return items.findAll().stream()
                .sorted(Comparator.comparing((FeedItem i) -> i.getName().toLowerCase(Locale.ROOT)))
                .map(this::toResponse)
                .toList();
    }

    public FeedItemResponse get(Long id) {
        return toResponse(find(id));
    }

    public FeedItemResponse create(FeedItemRequest request) {
        return create(request, time.today());
    }

    /** openingDate is the date given to the opening-stock movement (the demo data uses past dates). */
    public FeedItemResponse create(FeedItemRequest request, LocalDate openingDate) {
        String name = request.name().trim();
        if (items.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("name", "A feed item named '" + name + "' already exists.");
        }
        FeedItem item = items.save(new FeedItem(name, request.feedType().trim(),
                PerformanceCalculator.quantity(request.reorderThreshold()),
                FarmService.clean(request.supplier()), FarmService.clean(request.notes())));
        BigDecimal opening = request.openingQuantity() == null ? BigDecimal.ZERO
                : PerformanceCalculator.quantity(request.openingQuantity());
        if (opening.signum() > 0) {
            applyMovement(item, MovementType.STOCK_IN, opening, openingDate, null, "Opening stock", null);
        } else {
            alertService.evaluateLowStock(item);
        }
        return toResponse(item);
    }

    /** Edits descriptive fields and the reorder threshold. Quantity can NOT be edited here (use movements). */
    public FeedItemResponse update(Long id, FeedItemRequest request) {
        FeedItem item = find(id);
        String name = request.name().trim();
        Optional<FeedItem> sameName = items.findByNameIgnoreCase(name);
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            throw new ConflictException("name", "Another feed item is already named '" + name + "'.");
        }
        item.setName(name);
        item.setFeedType(request.feedType().trim());
        item.setReorderThreshold(PerformanceCalculator.quantity(request.reorderThreshold()));
        item.setSupplier(FarmService.clean(request.supplier()));
        item.setNotes(FarmService.clean(request.notes()));
        items.saveAndFlush(item);
        alertService.evaluateLowStock(item);
        return toResponse(item);
    }

    public CreateResult<FeedMovementResponse> stockIn(Long itemId, StockInRequest request) {
        String operationId = FarmService.clean(request.clientOperationId());
        Optional<FeedMovementResponse> replay = replayIfAlreadyProcessed(operationId, itemId, MovementType.STOCK_IN);
        if (replay.isPresent()) {
            return CreateResult.duplicate(replay.get());
        }
        FeedItem item = find(itemId);
        LocalDate date = resolveDate(request.movementDate());
        FeedMovement saved = applyMovement(item, MovementType.STOCK_IN, PerformanceCalculator.quantity(request.quantity()),
                date, null, FarmService.clean(request.notes()), operationId);
        return CreateResult.created(FeedMovementResponse.from(saved));
    }

    public CreateResult<FeedMovementResponse> recordUsage(Long itemId, UsageRequest request) {
        String operationId = FarmService.clean(request.clientOperationId());
        Optional<FeedMovementResponse> replay = replayIfAlreadyProcessed(operationId, itemId, MovementType.USAGE);
        if (replay.isPresent()) {
            return CreateResult.duplicate(replay.get());
        }
        FeedItem item = find(itemId);
        LocalDate date = resolveDate(request.movementDate());
        BigDecimal quantity = PerformanceCalculator.quantity(request.quantity());

        Flock flock = null;
        if (request.flockId() != null) {
            flock = flocks.findById(request.flockId())
                    .orElseThrow(() -> new BusinessRuleException("flockId", "The selected flock does not exist."));
            if (date.isBefore(flock.getPlacementDate())) {
                throw new BusinessRuleException("movementDate", "Feed usage cannot be dated before the flock's placement date ("
                        + flock.getPlacementDate() + ").");
            }
        }
        if (quantity.compareTo(item.getQuantityOnHand()) > 0) {
            throw new BusinessRuleException("quantity", String.format(Locale.ROOT,
                    "Not enough stock: you tried to use %s kg but only %s kg of '%s' is on hand.",
                    quantity.toPlainString(), item.getQuantityOnHand().toPlainString(), item.getName()));
        }
        FeedMovement saved = applyMovement(item, MovementType.USAGE, quantity, date, flock,
                FarmService.clean(request.notes()), operationId);
        return CreateResult.created(FeedMovementResponse.from(saved));
    }

    /** Movement history for one item, newest first. */
    public List<FeedMovementResponse> movements(Long itemId) {
        find(itemId);   // 404 if missing
        return movements.findByFeedItemId(itemId).stream().sorted(NEWEST_FIRST).map(FeedMovementResponse::from).toList();
    }

    // ---------------------------------------------------------------- internals

    /** The ONLY method that changes quantityOnHand. Callers must already have validated the quantity. */
    private FeedMovement applyMovement(FeedItem item, MovementType type, BigDecimal quantity, LocalDate date,
                                       Flock flock, String notes, String operationId) {
        BigDecimal newBalance = type == MovementType.STOCK_IN
                ? item.getQuantityOnHand().add(quantity)
                : item.getQuantityOnHand().subtract(quantity);
        item.setQuantityOnHand(newBalance);
        items.save(item);
        FeedMovement saved = movements.save(new FeedMovement(item, type, quantity, item.getQuantityOnHand(),
                date, flock, notes, operationId));
        alertService.evaluateLowStock(item);
        return saved;
    }

    private Optional<FeedMovementResponse> replayIfAlreadyProcessed(String operationId, Long itemId, MovementType type) {
        if (operationId == null) {
            return Optional.empty();
        }
        Optional<FeedMovement> existing = movements.findByClientOperationId(operationId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        FeedMovement original = existing.get();
        if (!original.getFeedItem().getId().equals(itemId) || original.getMovementType() != type) {
            throw new ConflictException("clientOperationId", "This operation id was already used for a different operation.");
        }
        return Optional.of(FeedMovementResponse.from(original));
    }

    private LocalDate resolveDate(LocalDate requested) {
        LocalDate date = requested == null ? time.today() : requested;
        if (date.isAfter(time.today())) {
            throw new BusinessRuleException("movementDate", "Date cannot be in the future.");
        }
        return date;
    }

    FeedItem find(Long id) {
        return items.findById(id).orElseThrow(() -> new NotFoundException("Feed item " + id + " was not found."));
    }

    private FeedItemResponse toResponse(FeedItem item) {
        return FeedItemResponse.from(item, PerformanceCalculator.isLowStock(item.getQuantityOnHand(), item.getReorderThreshold()));
    }
}
