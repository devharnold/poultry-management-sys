package com.poultry.pms.repository;

import com.poultry.pms.entity.MortalityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MortalityRecordRepository extends JpaRepository<MortalityRecord, Long> {
    Optional<MortalityRecord> findByClientOperationId(String clientOperationId);
    List<MortalityRecord> findByFlockId(Long flockId);
    List<MortalityRecord> findByRecordDate(LocalDate recordDate);
}
