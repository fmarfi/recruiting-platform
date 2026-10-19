package com.recruiting.platform.accounts.shared;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Binds a PATCH body to a request record, remembers which fields were sent, and validates it. */
@Component
public class PatchBinder {

    private final ObjectMapper mapper;
    private final Validator validator;

    public PatchBinder(ObjectMapper mapper, Validator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    public <T> Patch<T> bind(JsonNode json, Class<T> type) {
        if (!(json instanceof ObjectNode node)) {
            throw validation("body", "TYPE", "must be a JSON object");
        }
        T body;
        try {
            body = mapper.treeToValue(node, type);
        } catch (RuntimeException e) {
            throw validation("body", "TYPE", "malformed value");
        }
        Set<String> present = new HashSet<>();
        node.propertyNames().forEach(present::add);
        List<Map<String, String>> errors = new ArrayList<>();
        for (ConstraintViolation<T> v : validator.validate(body)) {
            errors.add(error(v.getPropertyPath().toString(),
                    code(v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()),
                    v.getMessage()));
        }
        if (!errors.isEmpty()) {
            throw validation(errors);
        }
        return new Patch<>(body, present);
    }

    /** Fails with VALIDATION_FAILED when a non-nullable field was sent as null. */
    public static void notNull(Patch<?> patch, String field, Object value) {
        if (patch.has(field) && value == null) {
            throw validation(field, "REQUIRED", "must not be null");
        }
    }

    public static ApiException validation(List<Map<String, String>> errors) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, "Validation failed.", Map.of("errors", errors));
    }

    public static ApiException validation(String field, String code, String message) {
        return validation(List.of(error(field, code, message)));
    }

    private static Map<String, String> error(String field, String code, String message) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("field", field);
        m.put("code", code);
        m.put("message", message);
        return m;
    }

    private static String code(String annotation) {
        return switch (annotation) {
            case "NotBlank", "NotNull", "NotEmpty" -> "REQUIRED";
            default -> annotation.toUpperCase(java.util.Locale.ROOT);
        };
    }
}
