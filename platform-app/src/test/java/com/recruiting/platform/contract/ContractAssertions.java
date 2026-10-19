package com.recruiting.platform.contract;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Validates MockMvc responses against contracts/openapi.yaml.
 *
 * <pre>
 * mockMvc.perform(get("/api/v1/auth/me"))
 *     .andExpect(status().isOk())
 *     .andExpect(ContractAssertions.matchesContract());
 * </pre>
 *
 * matchesContract() checks the request path and method exist in the spec, and that the response status,
 * content type and body match the operation. Works with standaloneSetup, @WebMvcTest and @AutoConfigureMockMvc.
 */
public final class ContractAssertions {

    private static volatile OpenApiInteractionValidator validator;

    private ContractAssertions() {}

    /** Locates contracts/openapi.yaml by walking up from the working directory. */
    static Path specPath() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("contracts").resolve("openapi.yaml");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("contracts/openapi.yaml not found above " + Path.of("").toAbsolutePath());
    }

    /** Shared validator (the spec is parsed once per JVM). Requests under /api/v1 map via the spec's servers entry. */
    public static OpenApiInteractionValidator validator() {
        OpenApiInteractionValidator v = validator;
        if (v == null) {
            synchronized (ContractAssertions.class) {
                if (validator == null) {
                    validator = OpenApiInteractionValidator.createFor(specPath().toUri().toString()).build();
                }
                v = validator;
            }
        }
        return v;
    }

    /** Result matcher: the response must be valid for its operation, status and content type. */
    public static ResultMatcher matchesContract() {
        return openApi().isValid(validator());
    }
}
