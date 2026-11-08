package com.poultry.pms.repository;

import com.poultry.pms.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    List<WeightRecord> findByFlockId(Long flockId);
    boolean existsByFlockIdAndRecordDate(Long flockId, LocalDate recordDate);
}
