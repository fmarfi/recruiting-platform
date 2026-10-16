package com.recruiting.demo.web;

import com.recruiting.demo.crm.CrmException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns {@link CrmException} into an RFC 9457 {@link ProblemDetail} response so the UI
 * can show the {@code detail} text to the user.
 */
@RestControllerAdvice
public class ApiErrors {

    /**
     * Maps a business error to its HTTP status and problem body.
     *
     * @param e the business error
     * @return the problem response
     */
    @ExceptionHandler(CrmException.class)
    public ResponseEntity<ProblemDetail> handle(CrmException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
        problem.setTitle(e.title());
        return ResponseEntity.status(e.status()).body(problem);
    }
}
