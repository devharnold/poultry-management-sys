package com.poultry.pms.dto;

public record DemoSeedResponse(boolean seeded, String message, int farms, int flocks, int mortalityRecords, int feedItems) {
}
