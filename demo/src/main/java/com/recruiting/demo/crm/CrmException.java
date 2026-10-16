package com.recruiting.demo.crm;

import org.springframework.http.HttpStatus;

/**
 * A business error the API reports as a {@code ProblemDetail}.
 */
public class CrmException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    private CrmException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    /**
     * A record that does not exist.
     *
     * @param what the kind of record, e.g. "Account"
     * @param id   the id that was asked for
     * @return the exception (404)
     */
    public static CrmException notFound(String what, String id) {
        return new CrmException(HttpStatus.NOT_FOUND, what + " not found",
                what + " " + id + " does not exist.");
    }

    /**
     * A status move that the lifecycle does not allow.
     *
     * @param detail what was attempted, in words a user understands
     * @return the exception (409)
     */
    public static CrmException illegalTransition(String detail) {
        return new CrmException(HttpStatus.CONFLICT, "Illegal status change", detail);
    }

    /**
     * Invalid or missing input.
     *
     * @param detail what is wrong and how to fix it
     * @return the exception (400)
     */
    public static CrmException invalid(String detail) {
        return new CrmException(HttpStatus.BAD_REQUEST, "Invalid input", detail);
    }

    /**
     * Returns the HTTP status to answer with.
     *
     * @return the HTTP status
     */
    public HttpStatus status() {
        return status;
    }

    /**
     * Returns the short title of the problem.
     *
     * @return the title
     */
    public String title() {
        return title;
    }
}
