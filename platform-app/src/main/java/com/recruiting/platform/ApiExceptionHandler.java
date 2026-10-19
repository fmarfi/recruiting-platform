package com.recruiting.platform;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.security.TooManyRequestsException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every expected error into an RFC 9457 problem (ADR 0006): {@code type}, {@code title}, {@code status},
 * {@code detail}, {@code code} and the extension members of the {@link ApiException}.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApi(ApiException ex) {
        ProblemDetail problem = problem(ex.code(), ex.getMessage());
        ex.properties().forEach(problem::setProperty);
        return ResponseEntity.status(ex.code().httpStatus()).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(problem(ErrorCode.FORBIDDEN, "Your role does not allow this action."));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Object> handleLock(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(problem(ErrorCode.VERSION_CONFLICT, "The record was changed by someone else. Reload and retry."));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraint(ConstraintViolationException ex) {
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED, "Validation failed.");
        problem.setProperty("errors", ex.getConstraintViolations().stream()
                .map(v -> Map.of("field", v.getPropertyPath().toString(), "code", "INVALID", "message", v.getMessage()))
                .toList());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<Object> handleRateLimit(TooManyRequestsException ex) {
        ProblemDetail problem = problem(ErrorCode.TOO_MANY_REQUESTS, ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "60").body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleOther(Exception ex) {
        LOG.error("Unhandled error", ex);
        return ResponseEntity.internalServerError()
                .body(problem(ErrorCode.INTERNAL_ERROR, "Something went wrong. Try again later."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED, "Validation failed.");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "code", code(e.getCode()),
                        "message", String.valueOf(e.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** Every other Spring MVC failure (unreadable body, bad parameter, 404, 405 ...) keeps its status. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            if (status.value() == 400) {
                problem.setType(URI.create(ErrorCode.VALIDATION_FAILED.typeUri()));
                problem.setProperty("code", ErrorCode.VALIDATION_FAILED.name());
            } else if (status.value() == 404) {
                problem.setType(URI.create(ErrorCode.NOT_FOUND.typeUri()));
                problem.setProperty("code", ErrorCode.NOT_FOUND.name());
            }
        }
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(code.httpStatus()), detail);
        problem.setType(URI.create(code.typeUri()));
        problem.setTitle(title(code));
        problem.setProperty("code", code.name());
        return problem;
    }

    private static String title(ErrorCode code) {
        String text = code.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String code(String constraint) {
        return switch (constraint == null ? "" : constraint) {
            case "NotNull", "NotBlank", "NotEmpty" -> "REQUIRED";
            case "Size", "Min", "Max", "Length" -> "SIZE";
            default -> "INVALID";
        };
    }
}
