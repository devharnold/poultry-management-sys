package com.poultry.pms.service;

/**
 * Result of an operation that can be retried safely.
 * duplicate = true means "this clientOperationId was already processed; here is the original record",
 * so the controller answers 200 OK instead of 201 Created and nothing is created twice.
 */
public record CreateResult<T>(T body, boolean duplicate) {

    public static <T> CreateResult<T> created(T body) {
        return new CreateResult<>(body, false);
    }

    public static <T> CreateResult<T> duplicate(T body) {
        return new CreateResult<>(body, true);
    }
}
