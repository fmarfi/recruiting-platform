package com.recruiting.platform.pipeline.bulk.app;

import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.pipeline.BulkParams;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.Transitions;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Checks a bulk request as a whole (400 on any problem) and turns {@code params} into {@link BulkParams}. */
final class BulkRequestParser {

    /** A valid request. */
    record Parsed(ListEntity entity, BulkActionType action, List<UUID> ids, BulkParams params) {
    }

    private BulkRequestParser() {
    }

    static Parsed parse(JsonNode body) {
        new Patch(body);
        ListEntity entity = enumValue(ListEntity.class, text(body, "entity"), "entity");
        BulkActionType action = enumValue(BulkActionType.class, text(body, "action"), "action");
        List<UUID> ids = ids(body.get("ids"));
        if (!allowed(entity, action)) {
            throw Transitions.invalid("action", action + " is not allowed for " + entity + ".");
        }
        JsonNode params = body.get("params");
        if (params == null || !params.isObject()) {
            throw Transitions.invalid("params", "params must be an object.");
        }
        Patch p = new Patch(params);
        Set<String> known = switch (action) {
            case REASSIGN_OWNER -> Set.of("ownerId");
            case CHANGE_STATUS -> Set.of("status", "reason");
            case ADD_TAG, REMOVE_TAG -> Set.of("tag");
            case SET_NEXT_ACTION -> Set.of("title", "dueOn", "ownerId");
            case REQUEST_CONSENT -> Set.of("language");
        };
        for (String key : params.propertyNames()) {
            if (!known.contains(key)) {
                throw Transitions.invalid("params." + key, "Unknown parameter for " + action + ".");
            }
        }
        BulkParams result = switch (action) {
            case REASSIGN_OWNER -> new BulkParams(p.uuid("ownerId"), null, null, null, null, null, null);
            case CHANGE_STATUS -> new BulkParams(null, status(entity, p), reason(p), null, null, null, null);
            case ADD_TAG, REMOVE_TAG -> new BulkParams(null, null, null, tag(p), null, null, null);
            case SET_NEXT_ACTION -> nextAction(p);
            case REQUEST_CONSENT -> new BulkParams(null, null, null, null, null, null,
                    p.has("language") ? enumValue(Language.class, p.text("language", 1, 10, false), "params.language")
                            : Language.TR);
        };
        return new Parsed(entity, action, ids, result);
    }

    static boolean allowed(ListEntity entity, BulkActionType action) {
        return switch (action) {
            case REASSIGN_OWNER, ADD_TAG, REMOVE_TAG -> true;
            case CHANGE_STATUS -> entity != ListEntity.PEOPLE;
            case SET_NEXT_ACTION -> entity != ListEntity.PROPOSALS;
            case REQUEST_CONSENT -> entity == ListEntity.PEOPLE;
        };
    }

    private static BulkParams nextAction(Patch p) {
        String title = p.text("title", 1, 300, false).trim();
        if (title.isEmpty()) {
            throw Transitions.invalid("params.title", "title must not be blank.");
        }
        return new BulkParams(p.has("ownerId") ? p.uuid("ownerId") : null, null, null, null, title, p.date("dueOn"),
                null);
    }

    private static String status(ListEntity entity, Patch p) {
        String s = p.text("status", 1, 40, false);
        switch (entity) {
            case ACCOUNTS -> enumValue(AccountStatus.class, s, "params.status");
            case JOB_ORDERS -> enumValue(JobOrderStatus.class, s, "params.status");
            case PROPOSALS -> enumValue(ProposalStatus.class, s, "params.status");
            default -> throw Transitions.invalid("action", "CHANGE_STATUS is not allowed for " + entity + ".");
        }
        return s;
    }

    private static String reason(Patch p) {
        if (!p.has("reason")) {
            return null;
        }
        String r = p.text("reason", 0, 500, true);
        if (r == null || r.trim().isEmpty()) {
            return null;
        }
        String t = r.trim();
        if (t.length() < 3) {
            throw Transitions.invalid("params.reason", "reason must be 3 to 500 characters.");
        }
        return t;
    }

    private static String tag(Patch p) {
        String t = p.text("tag", 1, 50, false);
        if (!t.equals(t.strip())) {
            throw Transitions.invalid("params.tag", "tag must not start or end with a space.");
        }
        return t.toLowerCase(Locale.ROOT);
    }

    private static List<UUID> ids(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty() || node.size() > 500) {
            throw Transitions.invalid("ids", "ids must hold 1 to 500 ids.");
        }
        Set<UUID> ids = new LinkedHashSet<>();
        for (JsonNode n : node) {
            try {
                if (!n.isString() || !ids.add(UUID.fromString(n.asString()))) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                throw Transitions.invalid("ids", "ids must be unique UUIDs.");
            }
        }
        return new ArrayList<>(ids);
    }

    private static String text(JsonNode body, String field) {
        JsonNode v = body.get(field);
        if (v == null || !v.isString()) {
            throw Transitions.invalid(field, field + " is required.");
        }
        return v.asString();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, field + " is not a valid value.",
                    java.util.Map.of("errors", List.of(java.util.Map.of("field", field, "code", "INVALID",
                            "message", field + " is not a valid value."))));
        }
    }
}
