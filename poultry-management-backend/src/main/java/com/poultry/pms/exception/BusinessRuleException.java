package com.poultry.pms.exception;

/**
 * A business rule was broken, e.g. mortality larger than the birds available (HTTP 422).
 * The optional field name lets the frontend show the message next to the right form field.
 */
public class BusinessRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String field;

    public BusinessRuleException(String message) {
        this(null, message);
    }

    public BusinessRuleException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
