package com.poultry.pms.repository;

import com.poultry.pms.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FarmRepository extends JpaRepository<Farm, Long> {
    Optional<Farm> findByNameIgnoreCase(String name);
}
