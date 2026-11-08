package com.poultry.pms.repository;

import com.poultry.pms.entity.FeedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {
    Optional<FeedItem> findByNameIgnoreCase(String name);
}
