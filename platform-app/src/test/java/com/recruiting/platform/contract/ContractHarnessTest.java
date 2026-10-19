package com.recruiting.platform.contract;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.mockmvc.OpenApiMatchers.OpenApiValidationException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proves the harness against a stub controller (no real endpoint existed when written):
 * a conforming response passes, a non-conforming one fails. Copy the pattern for real controllers.
 */
class ContractHarnessTest {

    @RestController
    @RequestMapping("/api/v1")
    static class StubController {
        @GetMapping(value = "/auth/me", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> me() {
            return Map.of(
                    "id", "0192a5b8-6f1e-7c3a-9b2d-3f4e5a6b7c8d",
                    "email", "consultant@demo.example",
                    "name", "Demo Consultant",
                    "role", "CONSULTANT",
                    "language", "TR",
                    "mustChangePassword", false);
        }

        @GetMapping(value = "/auth/me", produces = MediaType.APPLICATION_JSON_VALUE, params = "broken")
        Map<String, Object> broken() {
            return Map.of("id", "not-a-uuid", "role", "SUPERUSER");
        }
    }

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new StubController()).build();

    @Test
    void conformingResponseIsValid() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(ContractAssertions.matchesContract());
    }

    @Test
    void nonConformingResponseIsRejected() {
        OpenApiValidationException failure = assertThrows(OpenApiValidationException.class,
                () -> mockMvc.perform(get("/api/v1/auth/me").param("broken", ""))
                        .andExpect(ContractAssertions.matchesContract()));
        assertTrue(failure.getValidationReport().hasErrors());
        assertTrue(failure.getMessage().contains("SUPERUSER"), failure.getMessage());
    }
}
