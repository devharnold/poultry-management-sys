package com.poultry.pms.repository;

import com.poultry.pms.entity.Alert;
import com.poultry.pms.entity.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Optional<Alert> findFirstByDedupKeyAndStatus(String dedupKey, AlertStatus status);
    List<Alert> findByStatus(AlertStatus status);
}
