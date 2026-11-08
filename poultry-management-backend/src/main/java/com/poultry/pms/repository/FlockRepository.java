package com.poultry.pms.repository;

import com.poultry.pms.entity.Flock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlockRepository extends JpaRepository<Flock, Long> {
    Optional<Flock> findByFlockCodeIgnoreCase(String flockCode);
    long countByFarmId(Long farmId);
}
