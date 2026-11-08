package com.poultry.pms.exception;

/** The request clashes with existing data, e.g. a duplicate flock code (HTTP 409). */
public class ConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String field;

    public ConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
