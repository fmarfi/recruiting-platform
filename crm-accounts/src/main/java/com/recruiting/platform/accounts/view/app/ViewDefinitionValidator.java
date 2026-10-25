package com.recruiting.platform.accounts.view.app;

import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.view.web.dto.ViewDefinition;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.ProposalStatus;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Validates a view definition per entity (contract ViewDefinition): filter keys are the query parameters of the
 * entity's list endpoint, values are checked like those parameters; columns, sort and groupBy use the property
 * names of the list item schema. Errors use {@code field = definition.filters.<key>} and friends.
 */
final class ViewDefinitionValidator {

    private static final Pattern KEY = Pattern.compile("^[A-Za-z][A-Za-z0-9]{0,49}$");
    private static final Pattern COLUMN = Pattern.compile("^[A-Za-z][A-Za-z0-9.]{0,99}$");
    private static final Pattern SORT = Pattern.compile("^[A-Za-z]+(,(asc|desc))?$");

    private static final Predicate<String> ANY = s -> true;
    private static final Predicate<String> UUID_OR_ME = s -> "me".equals(s) || isUuid(s);
    private static final Predicate<String> UUID_ONLY = ViewDefinitionValidator::isUuid;
    private static final Predicate<String> DATE = ViewDefinitionValidator::isDate;
    private static final Predicate<String> BOOL = s -> "true".equals(s) || "false".equals(s);

    private static final Map<ListEntity, Map<String, Predicate<String>>> FILTERS = Map.of(
            ListEntity.PEOPLE, Map.of("q", ANY, "type", enumOf(PersonType.class), "ownerId", UUID_OR_ME,
                    "consentStatus", enumOf(ConsentStatus.class), "lastContactBefore", DATE,
                    "lastContactAfter", DATE),
            ListEntity.ACCOUNTS, Map.of("q", ANY, "status", enumOf(AccountStatus.class), "sector", ANY,
                    "ownerId", UUID_OR_ME),
            ListEntity.JOB_ORDERS, Map.of("q", ANY, "accountId", UUID_ONLY,
                    "status", enumOf(JobOrderStatus.class), "open", BOOL, "ownerId", UUID_OR_ME),
            ListEntity.PROPOSALS, Map.of("accountId", UUID_ONLY, "status", enumOf(ProposalStatus.class),
                    "ownerId", UUID_OR_ME));

    private static final Map<ListEntity, Set<String>> PROPERTIES = Map.of(
            ListEntity.PEOPLE, Set.of("id", "name", "title", "company", "type", "city", "email", "owner",
                    "lastContactAt", "consentStatus", "retentionStatus"),
            ListEntity.ACCOUNTS, Set.of("id", "name", "sector", "city", "website", "notes", "owner", "status",
                    "statusChangedAt", "openedOn", "openJobCount", "lastActivityAt", "createdAt", "updatedAt",
                    "version"),
            ListEntity.JOB_ORDERS, Set.of("id", "account", "title", "location", "seniority", "description",
                    "feeTerms", "openings", "status", "statusChangedAt", "owner", "openedOn", "closedOn", "daysOpen",
                    "activeCandidates", "stageCounts", "createdAt", "updatedAt", "version"),
            ListEntity.PROPOSALS, Set.of("id", "account", "title", "terms", "feeAmount", "currency", "status",
                    "statusChangedAt", "statusReason", "sentOn", "decidedOn", "owner", "createdAt", "updatedAt",
                    "version"));

    private ViewDefinitionValidator() {
    }

    static void validate(ListEntity entity, ViewDefinition d) {
        List<Map<String, String>> errors = new ArrayList<>();
        Map<String, Predicate<String>> allowed = FILTERS.get(entity);
        Set<String> props = PROPERTIES.get(entity);

        if (d.filters().size() > 20) {
            add(errors, "definition.filters", "TOO_MANY", "at most 20 filters");
        }
        d.filters().forEach((key, value) -> {
            String field = "definition.filters." + key;
            if (!KEY.matcher(key).matches() || !allowed.containsKey(key)) {
                add(errors, field, "UNKNOWN_FILTER",
                        "not a filter of " + entity + "; allowed: " + String.join(", ", new TreeSet<>(allowed.keySet())));
                return;
            }
            checkValue(errors, field, value, allowed.get(key));
        });

        if (d.columns().size() > 50) {
            add(errors, "definition.columns", "TOO_MANY", "at most 50 columns");
        }
        for (String c : d.columns()) {
            if (c == null || !COLUMN.matcher(c).matches() || !props.contains(root(c, '.'))) {
                add(errors, "definition.columns", "UNKNOWN_PROPERTY", "unknown column: " + c);
            }
        }
        if (d.sort().size() > 5) {
            add(errors, "definition.sort", "TOO_MANY", "at most 5 sort keys");
        }
        for (String s : d.sort()) {
            if (s == null || !SORT.matcher(s).matches() || !props.contains(root(s, ','))) {
                add(errors, "definition.sort", "UNKNOWN_PROPERTY", "unknown sort key: " + s);
            }
        }
        String g = d.groupBy();
        if (g != null && (!COLUMN.matcher(g).matches() || !props.contains(root(g, '.')))) {
            add(errors, "definition.groupBy", "UNKNOWN_PROPERTY", "unknown property: " + g);
        }
        if (!errors.isEmpty()) {
            throw PatchBinder.validation(errors);
        }
    }

    private static void checkValue(List<Map<String, String>> errors, String field, Object value,
            Predicate<String> ok) {
        if (value == null) {
            return;
        }
        if (value instanceof List<?> list) {
            if (list.size() > 50) {
                add(errors, field, "TOO_MANY", "at most 50 values");
            }
            for (Object item : list) {
                if (item instanceof List<?> || item instanceof Map<?, ?>) {
                    add(errors, field, "INVALID", "nested values are not allowed");
                } else if (item != null) {
                    checkScalar(errors, field, item, ok);
                }
            }
        } else if (value instanceof Map<?, ?>) {
            add(errors, field, "INVALID", "must be a string, number, boolean or array of those");
        } else {
            checkScalar(errors, field, value, ok);
        }
    }

    private static void checkScalar(List<Map<String, String>> errors, String field, Object value,
            Predicate<String> ok) {
        String s = String.valueOf(value);
        if (value instanceof String && s.length() > 200) {
            add(errors, field, "TOO_LONG", "at most 200 characters");
        } else if (!ok.test(s)) {
            add(errors, field, "INVALID", "bad value: " + (s.length() > 50 ? s.substring(0, 50) : s));
        }
    }

    private static void add(List<Map<String, String>> errors, String field, String code, String message) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("field", field);
        m.put("code", code);
        m.put("message", message);
        errors.add(m);
    }

    private static String root(String s, char sep) {
        int i = s.indexOf(sep);
        return i < 0 ? s : s.substring(0, i);
    }

    private static <T extends Enum<T>> Predicate<String> enumOf(Class<T> type) {
        return s -> {
            try {
                Enum.valueOf(type, s);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        };
    }

    private static boolean isUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isDate(String s) {
        try {
            LocalDate.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
