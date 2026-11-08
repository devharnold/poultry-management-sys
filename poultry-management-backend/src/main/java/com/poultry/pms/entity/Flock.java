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
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A batch of birds placed on one farm on one date.
 * NOTE: the current bird count is not stored here. It is always derived as
 * initialBirdCount minus the sum of that flock's mortality records, so it can never drift out of sync.
 */
@Entity
@Table(name = "flock")
public class Flock extends BaseEntity {

    @Column(name = "flock_code", nullable = false, unique = true)
    private String flockCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "bird_type", nullable = false)
    private String birdType;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "placement_date", nullable = false)
    private LocalDate placementDate;

    @Column(name = "initial_bird_count", nullable = false)
    private int initialBirdCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FlockStatus status;

    @Column(name = "notes")
    private String notes;

    protected Flock() { }

    public Flock(String flockCode, Farm farm, String birdType, LocalDate placementDate,
                 int initialBirdCount, FlockStatus status, String notes) {
        this.flockCode = flockCode;
        this.farm = farm;
        this.birdType = birdType;
        this.placementDate = placementDate;
        this.initialBirdCount = initialBirdCount;
        this.status = status;
        this.notes = notes;
    }

    public String getFlockCode() { return flockCode; }
    public void setFlockCode(String flockCode) { this.flockCode = flockCode; }
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public String getBirdType() { return birdType; }
    public void setBirdType(String birdType) { this.birdType = birdType; }
    public LocalDate getPlacementDate() { return placementDate; }
    public void setPlacementDate(LocalDate placementDate) { this.placementDate = placementDate; }
    public int getInitialBirdCount() { return initialBirdCount; }
    public void setInitialBirdCount(int initialBirdCount) { this.initialBirdCount = initialBirdCount; }
    public FlockStatus getStatus() { return status; }
    public void setStatus(FlockStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
