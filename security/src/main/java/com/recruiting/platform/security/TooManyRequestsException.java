package com.recruiting.platform.security;

/** Thrown when a rate limit is exceeded; the global handler answers 429 (not an ErrorCode in the contract yet). */
public class TooManyRequestsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TooManyRequestsException() {
        super("Too many requests. Try again later.");
    }
}
