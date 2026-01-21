package com.vaultstream.customer.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking business metrics related to customer operations.
 * 
 * Provides counters and gauges for monitoring customer lifecycle events
 * and system health.
 */
@Slf4j
@ApplicationScoped
public class CustomerMetrics {

    private final Counter customerCreatedCounter;
    private final Counter customerActivatedCounter;
    private final Counter customerSuspendedCounter;
    private final Counter customerDeactivatedCounter;
    private final Counter customerUpdatedCounter;

    private final AtomicLong activeCustomerCount = new AtomicLong(0);

    @Inject
    public CustomerMetrics(MeterRegistry registry) {
        // Counters for customer lifecycle events
        this.customerCreatedCounter = Counter.builder("vaultstream.customer.created")
                .description("Total number of customers created")
                .tag("service", "customer-service")
                .register(registry);

        this.customerActivatedCounter = Counter.builder("vaultstream.customer.activated")
                .description("Total number of customers activated")
                .tag("service", "customer-service")
                .register(registry);

        this.customerSuspendedCounter = Counter.builder("vaultstream.customer.suspended")
                .description("Total number of customers suspended")
                .tag("service", "customer-service")
                .register(registry);

        this.customerDeactivatedCounter = Counter.builder("vaultstream.customer.deactivated")
                .description("Total number of customers deactivated")
                .tag("service", "customer-service")
                .register(registry);

        this.customerUpdatedCounter = Counter.builder("vaultstream.customer.updated")
                .description("Total number of customer updates")
                .tag("service", "customer-service")
                .register(registry);

        // Gauge for active customer count
        Gauge.builder("vaultstream.customer.active.count", activeCustomerCount, AtomicLong::get)
                .description("Current number of active customers")
                .tag("service", "customer-service")
                .register(registry);

        log.info("Customer metrics initialized");
    }

    /**
     * Increment counter when a customer is created
     */
    public void recordCustomerCreated() {
        customerCreatedCounter.increment();
        log.debug("Customer created counter incremented");
    }

    /**
     * Increment counter when a customer is activated
     */
    public void recordCustomerActivated() {
        customerActivatedCounter.increment();
        activeCustomerCount.incrementAndGet();
        log.debug("Customer activated counter incremented, active count: {}", activeCustomerCount.get());
    }

    /**
     * Increment counter when a customer is suspended
     */
    public void recordCustomerSuspended() {
        customerSuspendedCounter.increment();
        activeCustomerCount.decrementAndGet();
        log.debug("Customer suspended counter incremented, active count: {}", activeCustomerCount.get());
    }

    /**
     * Increment counter when a customer is deactivated
     */
    public void recordCustomerDeactivated() {
        customerDeactivatedCounter.increment();
        activeCustomerCount.decrementAndGet();
        log.debug("Customer deactivated counter incremented, active count: {}", activeCustomerCount.get());
    }

    /**
     * Increment counter when a customer is updated
     */
    public void recordCustomerUpdated() {
        customerUpdatedCounter.increment();
        log.debug("Customer updated counter incremented");
    }

    /**
     * Update the active customer count gauge
     * (used for initialization or periodic sync)
     */
    public void setActiveCustomerCount(long count) {
        activeCustomerCount.set(count);
        log.debug("Active customer count set to: {}", count);
    }

    /**
     * Get current active customer count
     */
    public long getActiveCustomerCount() {
        return activeCustomerCount.get();
    }
}
