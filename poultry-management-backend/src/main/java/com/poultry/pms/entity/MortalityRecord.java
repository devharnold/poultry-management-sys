package com.poultry.pms.entity;

import com.poultry.pms.entity.converter.LocalDateStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Deaths recorded for one flock on one date. */
@Entity
@Table(name = "mortality_record")
public class MortalityRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flock_id", nullable = false)
    private Flock flock;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "death_count", nullable = false)
    private int deathCount;

    @Column(name = "reason")
    private String reason;

    /** Client-generated UUID used to ignore duplicate retries from the offline queue. */
    @Column(name = "client_operation_id", unique = true)
    private String clientOperationId;

    protected MortalityRecord() { }

    public MortalityRecord(Flock flock, LocalDate recordDate, int deathCount, String reason, String clientOperationId) {
        this.flock = flock;
        this.recordDate = recordDate;
        this.deathCount = deathCount;
        this.reason = reason;
        this.clientOperationId = clientOperationId;
    }

    public Flock getFlock() { return flock; }
    public LocalDate getRecordDate() { return recordDate; }
    public int getDeathCount() { return deathCount; }
    public String getReason() { return reason; }
    public String getClientOperationId() { return clientOperationId; }
}
