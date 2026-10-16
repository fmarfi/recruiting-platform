package com.recruiting.platform.contracts;

import java.util.Map;

/**
 * Base exception every module throws for an expected error. The global handler in
 * {@code platform-app} turns it into an RFC 9457 problem response with
 * {@code type = code.typeUri()}, {@code status = code.httpStatus()}, {@code code},
 * {@code detail} and the extra properties (ADR 0006).
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final transient Map<String, Object> properties;

    /**
     * Creates an exception without extra properties.
     *
     * @param code   the error code
     * @param detail human-readable explanation for this occurrence
     */
    public ApiException(ErrorCode code, String detail) {
        this(code, detail, Map.of());
    }

    /**
     * Creates an exception with extra problem properties.
     *
     * @param code       the error code
     * @param detail     human-readable explanation for this occurrence
     * @param properties extension members added to the problem body
     */
    public ApiException(ErrorCode code, String detail, Map<String, Object> properties) {
        super(detail);
        this.code = code;
        this.properties = Map.copyOf(properties);
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public ErrorCode code() {
        return code;
    }

    /**
     * Returns the extension members for the problem body.
     *
     * @return an unmodifiable map, possibly empty
     */
    public Map<String, Object> properties() {
        return properties;
    }
}
