package com.recruiting.platform.pipeline.agreement.app;

import com.recruiting.platform.pipeline.shared.Transitions;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/** Typed reads of a JSON object with "missing" and "null" told apart; every failure is a 400 VALIDATION_FAILED. */
final class Body {

    private static final Pattern MONEY = Pattern.compile("^[0-9]+([.][0-9]{1,2})?$");
    private static final Pattern PERCENT = Pattern.compile("^[0-9]{1,3}([.][0-9]{1,2})?$");

    private final JsonNode node;

    Body(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw Transitions.invalid("body", "The request body must be a JSON object.");
        }
        this.node = node;
    }

    boolean has(String f) {
        return node.has(f);
    }

    private JsonNode value(String f) {
        JsonNode v = node.get(f);
        return v == null || v.isNull() ? null : v;
    }

    String text(String f, int max, boolean nullable) {
        JsonNode v = value(f);
        if (v == null) {
            if (!nullable) {
                throw Transitions.invalid(f, f + " must not be null.");
            }
            return null;
        }
        if (!v.isString() || v.asString().length() > max) {
            throw Transitions.invalid(f, f + " must be a string of at most " + max + " characters.");
        }
        return v.asString();
    }

    <E extends Enum<E>> E enumValue(String f, Class<E> type, boolean nullable) {
        String s = text(f, 60, nullable);
        if (s == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, s);
        } catch (IllegalArgumentException e) {
            throw Transitions.invalid(f, f + " has an unknown value.");
        }
    }

    LocalDate date(String f) {
        String s = text(f, 10, true);
        try {
            return s == null ? null : LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw Transitions.invalid(f, f + " must be a date (YYYY-MM-DD).");
        }
    }

    UUID uuid(String f) {
        String s = text(f, 40, true);
        try {
            return s == null ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw Transitions.invalid(f, f + " must be a UUID.");
        }
    }

    BigDecimal money(String f) {
        String s = text(f, 30, true);
        if (s != null && !MONEY.matcher(s).matches()) {
            throw Transitions.invalid(f, f + " must be a decimal with up to 2 digits.");
        }
        return s == null ? null : new BigDecimal(s);
    }

    BigDecimal percent(String f) {
        String s = text(f, 10, true);
        if (s == null) {
            return null;
        }
        if (!PERCENT.matcher(s).matches() || new BigDecimal(s).compareTo(BigDecimal.valueOf(100)) > 0) {
            throw Transitions.invalid(f, f + " must be a percent from 0 to 100.");
        }
        return new BigDecimal(s);
    }

    Integer integer(String f, int min, int max, boolean nullable) {
        JsonNode v = value(f);
        if (v == null) {
            if (!nullable) {
                throw Transitions.invalid(f, f + " must not be null.");
            }
            return null;
        }
        if (!v.isIntegralNumber() || v.asInt() < min || v.asInt() > max) {
            throw Transitions.invalid(f, f + " must be an integer from " + min + " to " + max + ".");
        }
        return v.asInt();
    }

    boolean bool(String f, boolean fallback) {
        JsonNode v = value(f);
        if (v == null) {
            return fallback;
        }
        if (!v.isBoolean()) {
            throw Transitions.invalid(f, f + " must be true or false.");
        }
        return v.asBoolean();
    }

    <E extends Enum<E>> List<E> enumList(String f, Class<E> type) {
        JsonNode v = value(f);
        List<E> out = new ArrayList<>();
        if (v == null) {
            return out;
        }
        if (!v.isArray()) {
            throw Transitions.invalid(f, f + " must be an array.");
        }
        for (JsonNode n : v) {
            try {
                E e = Enum.valueOf(type, n.asString());
                if (!out.contains(e)) {
                    out.add(e);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                throw Transitions.invalid(f, f + " holds an unknown value.");
            }
        }
        return out;
    }

    List<Body> objects(String f, int max) {
        JsonNode v = value(f);
        List<Body> out = new ArrayList<>();
        if (v == null) {
            if (node.has(f)) {
                throw Transitions.invalid(f, f + " must not be null.");
            }
            return out;
        }
        if (!v.isArray() || v.size() > max) {
            throw Transitions.invalid(f, f + " must be an array of at most " + max + " items.");
        }
        for (JsonNode n : v) {
            out.add(new Body(n));
        }
        return out;
    }

    Long version() {
        JsonNode v = value("version");
        if (v == null) {
            return null;
        }
        if (!v.isIntegralNumber()) {
            throw Transitions.invalid("version", "version must be an integer.");
        }
        return v.asLong();
    }
}
