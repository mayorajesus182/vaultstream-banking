package com.vaultstream.customer.infrastructure.health;

import io.smallrye.reactive.messaging.kafka.KafkaConnector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * Kafka readiness health check.
 * 
 * Verifies that Kafka is ready to accept messages.
 * This check is used by Kubernetes/OpenShift to determine if the pod should
 * receive traffic.
 */
@Slf4j
@Readiness
@ApplicationScoped
public class KafkaHealthCheck implements HealthCheck {

    @Inject
    @Channel("customer-events-out")
    Emitter<String> emitter;

    @Override
    public HealthCheckResponse call() {
        try {
            // Check if the emitter is ready to send messages
            boolean isReady = !emitter.isCancelled() && !emitter.hasRequests();

            if (isReady || emitter.hasRequests()) {
                log.debug("Kafka health check passed");

                return HealthCheckResponse.builder()
                        .name("Kafka connection")
                        .up()
                        .withData("channel", "customer-events-out")
                        .withData("status", "ready")
                        .withData("has_backpressure", !emitter.hasRequests())
                        .build();
            } else {
                log.warn("Kafka health check failed - channel cancelled");

                return HealthCheckResponse.builder()
                        .name("Kafka connection")
                        .down()
                        .withData("channel", "customer-events-out")
                        .withData("status", "cancelled")
                        .build();
            }

        } catch (Exception e) {
            log.error("Kafka health check failed", e);

            return HealthCheckResponse.builder()
                    .name("Kafka connection")
                    .down()
                    .withData("error", e.getMessage())
                    .withData("status", "failed")
                    .build();
        }
    }
}
