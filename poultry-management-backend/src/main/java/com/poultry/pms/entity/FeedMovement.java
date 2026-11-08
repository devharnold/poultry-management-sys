package com.poultry.pms.entity;

import com.poultry.pms.entity.converter.LocalDateStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * One change to a feed item's stock: STOCK_IN adds, USAGE subtracts.
 * quantityAfter records the balance right after the movement, so history explains every change.
 */
@Entity
@Table(name = "feed_movement")
public class FeedMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_item_id", nullable = false)
    private FeedItem feedItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "quantity_after", nullable = false)
    private BigDecimal quantityAfter;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    /** Which flock consumed the feed (optional; needed for feed conversion ratio). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flock_id")
    private Flock flock;

    @Column(name = "notes")
    private String notes;

    @Column(name = "client_operation_id", unique = true)
    private String clientOperationId;

    protected FeedMovement() { }

    public FeedMovement(FeedItem feedItem, MovementType movementType, BigDecimal quantity, BigDecimal quantityAfter,
                        LocalDate movementDate, Flock flock, String notes, String clientOperationId) {
        this.feedItem = feedItem;
        this.movementType = movementType;
        this.quantity = quantity;
        this.quantityAfter = quantityAfter;
        this.movementDate = movementDate;
        this.flock = flock;
        this.notes = notes;
        this.clientOperationId = clientOperationId;
    }

    @PostLoad
    @PrePersist
    @PreUpdate
    void normalizeQuantities() {
        quantity = quantity == null ? null : quantity.setScale(3, RoundingMode.HALF_UP);
        quantityAfter = quantityAfter == null ? null : quantityAfter.setScale(3, RoundingMode.HALF_UP);
    }

    public FeedItem getFeedItem() { return feedItem; }
    public MovementType getMovementType() { return movementType; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getQuantityAfter() { return quantityAfter; }
    public LocalDate getMovementDate() { return movementDate; }
    public Flock getFlock() { return flock; }
    public String getNotes() { return notes; }
    public String getClientOperationId() { return clientOperationId; }
}
