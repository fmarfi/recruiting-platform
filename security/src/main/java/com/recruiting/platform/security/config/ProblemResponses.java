package com.recruiting.platform.security.config;

import com.recruiting.platform.contracts.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Writes the 401 and 403 problem bodies (RFC 9457, ADR 0006) for failures that happen inside the filter chain. */
@Component
class ProblemResponses {

    AuthenticationEntryPoint unauthenticated() {
        return (request, response, ex) -> write(response, ErrorCode.UNAUTHENTICATED, "Not logged in",
                "Log in to use this endpoint.");
    }

    AccessDeniedHandler forbidden() {
        return (request, response, ex) -> write(response, ErrorCode.FORBIDDEN, "Not allowed",
                "Your role does not allow this action.");
    }

    private static void write(HttpServletResponse response, ErrorCode code, String title, String detail)
            throws IOException {
        response.setStatus(code.httpStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"" + code.typeUri() + "\",\"title\":\"" + title + "\",\"status\":"
                + code.httpStatus() + ",\"detail\":\"" + detail + "\",\"code\":\"" + code.name() + "\"}");
    }
}
