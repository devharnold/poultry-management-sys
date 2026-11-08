package com.poultry.pms.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/** The JSON shape of every error response. fieldErrors maps a field name to its message (may be null). */
public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> fieldErrors) {

    public static ApiError of(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
