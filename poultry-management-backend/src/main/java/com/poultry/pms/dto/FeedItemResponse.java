package com.poultry.pms.dto;

import com.poultry.pms.entity.FeedItem;

import java.math.BigDecimal;
import java.time.Instant;

public record FeedItemResponse(Long id, String name, String feedType, BigDecimal quantityOnHand, String unit,
                               BigDecimal reorderThreshold, boolean lowStock, String supplier, String notes,
                               Instant createdAt, Instant updatedAt) {

    public static FeedItemResponse from(FeedItem item, boolean lowStock) {
        return new FeedItemResponse(item.getId(), item.getName(), item.getFeedType(), item.getQuantityOnHand(),
                item.getUnit(), item.getReorderThreshold(), lowStock, item.getSupplier(), item.getNotes(),
                item.getCreatedAt(), item.getUpdatedAt());
    }
}
