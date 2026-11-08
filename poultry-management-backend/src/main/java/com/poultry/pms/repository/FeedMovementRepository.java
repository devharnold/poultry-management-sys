package com.poultry.pms.repository;

import com.poultry.pms.entity.FeedMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedMovementRepository extends JpaRepository<FeedMovement, Long> {
    Optional<FeedMovement> findByClientOperationId(String clientOperationId);
    List<FeedMovement> findByFeedItemId(Long feedItemId);
    List<FeedMovement> findByFlockId(Long flockId);
}
