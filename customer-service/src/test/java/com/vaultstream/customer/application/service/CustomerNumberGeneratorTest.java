package com.vaultstream.customer.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@DisplayName("Customer Number Generator")
class CustomerNumberGeneratorTest {

    @org.mockito.Mock
    com.vaultstream.customer.domain.repository.CustomerRepository customerRepository;

    @org.mockito.InjectMocks
    CustomerNumberGenerator generator;

    @Test
    @DisplayName("should generate valid customer number format")
    void shouldGenerateValidFormat() {
        org.mockito.Mockito.when(customerRepository.existsByCustomerNumber(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);

        String number = generator.generate();

        assertThat(number).startsWith("CUST-");
        assertThat(number.length()).isGreaterThan(10);
    }

    @Test
    @DisplayName("should generate unique numbers concurrently")
    void shouldGenerateUniqueNumbers() throws InterruptedException, ExecutionException {
        // For concurrency test, we need to ensure mock behaves correctly across threads
        org.mockito.Mockito.when(customerRepository.existsByCustomerNumber(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Set<String> numbers = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String num = generator.generate();
                    if (num != null)
                        numbers.add(num);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        assertThat(numbers).hasSize(threadCount);
        executor.shutdown();
    }
}
