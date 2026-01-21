package com.vaultstream.customer.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Database liveness health check.
 * 
 * Verifies that the database connection is alive by executing a simple query.
 * This check is used by Kubernetes/OpenShift to determine if the pod should be
 * restarted.
 */
@Slf4j
@Liveness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    @Inject
    EntityManager em;

    @Override
    public HealthCheckResponse call() {
        try {
            // Execute a simple query to verify database connectivity
            Long count = em.createQuery("SELECT COUNT(c) FROM CustomerEntity c", Long.class)
                    .getSingleResult();

            log.debug("Database health check passed. Customer count: {}", count);

            return HealthCheckResponse.builder()
                    .name("Database connection")
                    .up()
                    .withData("customer_count", count)
                    .withData("connection_status", "active")
                    .build();

        } catch (Exception e) {
            log.error("Database health check failed", e);

            return HealthCheckResponse.builder()
                    .name("Database connection")
                    .down()
                    .withData("error", e.getMessage())
                    .withData("connection_status", "failed")
                    .build();
        }
    }
}
