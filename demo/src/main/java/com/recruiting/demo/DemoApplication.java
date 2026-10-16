package com.recruiting.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the recruiting CRM demo.
 *
 * <p>The demo shows the Phase 1 CRM (Module A) end to end: accounts with status,
 * contacts, the firm's network, job orders with a candidate pipeline board, the
 * "My day" home screen and global search. All data is fictional and held in memory,
 * so every restart returns to the same demo state.
 *
 * <p>Run with {@code mvn spring-boot:run} and open {@code http://localhost:8080}.
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * Starts the demo server.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
