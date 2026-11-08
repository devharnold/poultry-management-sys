package com.poultry.pms.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Stores timestamps in SQLite as UTC text such as {@code 2026-11-02T08:30:00.123Z}.
 * A fixed number of decimals keeps the text sortable, and avoids SQLite/JDBC date quirks.
 */
@Converter
public class InstantStringConverter implements AttributeConverter<Instant, String> {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    @Override
    public String convertToDatabaseColumn(Instant value) {
        return value == null ? null : FORMAT.format(value);
    }

    @Override
    public Instant convertToEntityAttribute(String text) {
        return text == null ? null : Instant.parse(text);
    }
}
