package com.recruiting.platform.pipeline.shared;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * Reads a PATCH body so that "field missing" (no change) and "field is null" (clear it) can be told apart.
 */
public final class Patch {

    private final JsonNode node;

    /**
     * Wraps a body.
     *
     * @param node the JSON object
     */
    public Patch(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw Transitions.invalid("body", "The request body must be a JSON object.");
        }
        this.node = node;
    }

    /**
     * Tells whether the field is in the body (also when null).
     *
     * @param field name
     * @return {@code true} if present
     */
    public boolean has(String field) {
        return node.has(field);
    }

    /**
     * Reads a string field with a length limit.
     *
     * @param field    name
     * @param min      minimum length (0 allows empty)
     * @param max      maximum length
     * @param nullable whether JSON null is accepted
     * @return the string, or {@code null}
     */
    public String text(String field, int min, int max, boolean nullable) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            if (!nullable) {
                throw Transitions.invalid(field, field + " must not be null.");
            }
            return null;
        }
        if (!v.isString()) {
            throw Transitions.invalid(field, field + " must be a string.");
        }
        String s = v.asString();
        if (s.length() < min || s.length() > max) {
            throw Transitions.invalid(field, field + " must be " + min + " to " + max + " characters.");
        }
        return s;
    }

    /**
     * Reads an integer field.
     *
     * @param field name
     * @param min   minimum value
     * @return the number
     */
    public int integer(String field, int min) {
        JsonNode v = node.get(field);
        if (v == null || !v.isIntegralNumber() || v.asInt() < min) {
            throw Transitions.invalid(field, field + " must be an integer >= " + min + ".");
        }
        return v.asInt();
    }

    /**
     * Reads a boolean field.
     *
     * @param field name
     * @return the value
     */
    public boolean bool(String field) {
        JsonNode v = node.get(field);
        if (v == null || !v.isBoolean()) {
            throw Transitions.invalid(field, field + " must be true or false.");
        }
        return v.asBoolean();
    }

    /**
     * Reads a date field.
     *
     * @param field name
     * @return the date
     */
    public LocalDate date(String field) {
        try {
            return LocalDate.parse(requiredText(field));
        } catch (DateTimeParseException e) {
            throw Transitions.invalid(field, field + " must be a date (YYYY-MM-DD).");
        }
    }

    /**
     * Reads a date-time field.
     *
     * @param field name
     * @return the instant
     */
    public Instant instant(String field) {
        try {
            return Instant.parse(requiredText(field));
        } catch (DateTimeParseException e) {
            throw Transitions.invalid(field, field + " must be a date-time (RFC 3339, UTC).");
        }
    }

    /**
     * Reads an id field.
     *
     * @param field name
     * @return the id
     */
    public UUID uuid(String field) {
        try {
            return UUID.fromString(requiredText(field));
        } catch (IllegalArgumentException e) {
            throw Transitions.invalid(field, field + " must be a UUID.");
        }
    }

    /**
     * Reads the optional {@code version} field.
     *
     * @return the version, or {@code null} when absent
     */
    public Long version() {
        JsonNode v = node.get("version");
        if (v == null || v.isNull()) {
            return null;
        }
        if (!v.isIntegralNumber()) {
            throw Transitions.invalid("version", "version must be an integer.");
        }
        return v.asLong();
    }

    private String requiredText(String field) {
        JsonNode v = node.get(field);
        if (v == null || !v.isString()) {
            throw Transitions.invalid(field, field + " must be a string.");
        }
        return v.asString();
    }
}
