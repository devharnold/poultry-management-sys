package com.poultry.pms.entity;

import com.poultry.pms.entity.converter.LocalDateStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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

/** A sampled average bird weight for a flock on one date (needed for feed conversion ratio). */
@Entity
@Table(name = "weight_record")
public class WeightRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flock_id", nullable = false)
    private Flock flock;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "average_bird_weight_kg", nullable = false)
    private BigDecimal averageBirdWeightKg;

    @Column(name = "sampled_bird_count", nullable = false)
    private int sampledBirdCount;

    @Column(name = "notes")
    private String notes;

    protected WeightRecord() { }

    public WeightRecord(Flock flock, LocalDate recordDate, BigDecimal averageBirdWeightKg,
                        int sampledBirdCount, String notes) {
        this.flock = flock;
        this.recordDate = recordDate;
        this.averageBirdWeightKg = averageBirdWeightKg;
        this.sampledBirdCount = sampledBirdCount;
        this.notes = notes;
    }

    @PostLoad
    @PrePersist
    @PreUpdate
    void normalizeWeight() {
        averageBirdWeightKg = averageBirdWeightKg == null ? null : averageBirdWeightKg.setScale(3, RoundingMode.HALF_UP);
    }

    public Flock getFlock() { return flock; }
    public LocalDate getRecordDate() { return recordDate; }
    public BigDecimal getAverageBirdWeightKg() { return averageBirdWeightKg; }
    public int getSampledBirdCount() { return sampledBirdCount; }
    public String getNotes() { return notes; }
}
