package com.vaultstream.customer.application.service;

import com.vaultstream.customer.domain.repository.CustomerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for generating unique customer numbers.
 * 
 * Thread-safe implementation that generates customer numbers in the format:
 * CUST-YYYYMMDD-XXXXX
 * 
 * Where:
 * - YYYYMMDD is the current date
 * - XXXXX is a 5-digit sequence number
 */
@Slf4j
@ApplicationScoped
public class CustomerNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String PREFIX = "CUST-";
    private static final int SEQUENCE_LENGTH = 5;

    private final AtomicLong counter = new AtomicLong(1);

    @Inject
    CustomerRepository repository;

    /**
     * Generate a unique customer number.
     * 
     * This method is thread-safe and will retry if a collision is detected
     * (which should be extremely rare).
     * 
     * @return A unique customer number
     */
    public String generate() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String candidate;
        int attempts = 0;
        final int maxAttempts = 100;

        do {
            long sequence = counter.getAndIncrement();
            String sequencePart = String.format("%0" + SEQUENCE_LENGTH + "d", sequence % 100000);
            candidate = PREFIX + datePart + "-" + sequencePart;

            attempts++;
            if (attempts >= maxAttempts) {
                log.error("Failed to generate unique customer number after {} attempts", maxAttempts);
                throw new IllegalStateException(
                        "Unable to generate unique customer number. Please try again.");
            }

        } while (repository.existsByCustomerNumber(candidate));

        log.debug("Generated customer number: {}", candidate);
        return candidate;
    }

    /**
     * Reset the counter (for testing purposes only)
     */
    void resetCounter() {
        counter.set(1);
    }
}
