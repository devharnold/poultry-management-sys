package com.poultry.pms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A feed product in stock. Quantities are ALWAYS kilograms in this prototype.
 * quantityOnHand changes only through FeedMovement operations (stock-in / usage), never by direct edit.
 */
@Entity
@Table(name = "feed_item")
public class FeedItem extends BaseEntity {

    public static final String UNIT_KG = "KG";

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "feed_type", nullable = false)
    private String feedType;

    @Column(name = "quantity_on_hand", nullable = false)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "unit", nullable = false)
    private String unit = UNIT_KG;

    @Column(name = "reorder_threshold", nullable = false)
    private BigDecimal reorderThreshold = BigDecimal.ZERO;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "notes")
    private String notes;

    protected FeedItem() { }

    public FeedItem(String name, String feedType, BigDecimal reorderThreshold, String supplier, String notes) {
        this.name = name;
        this.feedType = feedType;
        this.reorderThreshold = reorderThreshold;
        this.supplier = supplier;
        this.notes = notes;
    }

    /** SQLite hands numbers back in its own precision; always work with exactly 3 decimals. */
    @PostLoad
    @PrePersist
    @PreUpdate
    void normalizeQuantities() {
        quantityOnHand = scale3(quantityOnHand);
        reorderThreshold = scale3(reorderThreshold);
    }

    private static BigDecimal scale3(BigDecimal value) {
        return value == null ? null : value.setScale(3, RoundingMode.HALF_UP);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFeedType() { return feedType; }
    public void setFeedType(String feedType) { this.feedType = feedType; }
    public BigDecimal getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(BigDecimal quantityOnHand) { this.quantityOnHand = scale3(quantityOnHand); }
    public String getUnit() { return unit; }
    public BigDecimal getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(BigDecimal reorderThreshold) { this.reorderThreshold = scale3(reorderThreshold); }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
