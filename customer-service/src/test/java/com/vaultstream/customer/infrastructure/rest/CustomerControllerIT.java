package com.vaultstream.customer.infrastructure.rest;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(CustomerControllerIT.NoCacheProfile.class)
@DisplayName("Customer REST API Integration")
@io.quarkus.test.security.TestSecurity(user = "admin", roles = "admin")
class CustomerControllerIT {

    /**
     * Test profile that disables caching to avoid Redis connection errors in tests
     */
    public static class NoCacheProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            // Disable all caching for this test
            config.put("quarkus.cache.enabled", "false");
            return config;
        }
    }

    private static final String API_BASE = "/api/v1/customers";

    @InjectMock
    RedisDataSource redisDataSource;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        // Mock Redis ValueCommands for Rate Limiting
        ValueCommands<String, Long> valueCommands = mock(ValueCommands.class);
        KeyCommands<String> keyCommands = mock(KeyCommands.class);

        when(redisDataSource.value(Long.class)).thenReturn(valueCommands);
        when(redisDataSource.key()).thenReturn(keyCommands);

        // Always allow rate limit (return 1 for first request)
        when(valueCommands.incr(anyString())).thenReturn(1L);
        when(valueCommands.get(anyString())).thenReturn(1L);
        when(keyCommands.expire(anyString(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("POST /customers should create customer")
    void shouldCreateCustomer() {
        String payload = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.it.h2@example.com",
                    "phoneNumber": "+1234567890",
                    "dateOfBirth": "1990-01-01",
                    "nationalId": "IT-H2-12345",
                    "address": {
                        "street": "Test St",
                        "number": "1",
                        "city": "Test City",
                        "state": "TS",
                        "postalCode": "12345",
                        "country": "USA"
                    },
                    "type": "INDIVIDUAL"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post(API_BASE)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("customerNumber", startsWith("CUST-"))
                .body("email", equalTo("john.it.h2@example.com"))
                .body("status", equalTo("PENDING_VERIFICATION"));
    }

    @Test
    @DisplayName("GET /customers/{id} should return customer")
    void shouldGetCustomer() {
        // First create
        String payload = """
                {
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "email": "jane.it.h2@example.com",
                    "phoneNumber": "+1234567890",
                    "dateOfBirth": "1990-01-01",
                    "nationalId": "IT-H2-67890",
                    "type": "INDIVIDUAL"
                }
                """;

        String id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post(API_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Then get
        given()
                .when()
                .get(API_BASE + "/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("firstName", equalTo("Jane"));
    }

    @Test
    @DisplayName("POST /customers should validate duplicate email")
    void shouldValidateDuplicateEmail() {
        String payload = """
                {
                    "firstName": "Dupe",
                    "lastName": "Test",
                    "email": "dupe.h2@example.com",
                    "phoneNumber": "+1234567890",
                    "dateOfBirth": "1990-01-01",
                    "nationalId": "IT-H2-DUPE1",
                    "type": "INDIVIDUAL"
                }
                """;

        // Create first
        given().contentType(ContentType.JSON).body(payload).post(API_BASE).then().statusCode(201);

        // Try create second with SAME EMAIL
        String payload2 = """
                {
                    "firstName": "Dupe",
                    "lastName": "Test",
                    "email": "dupe.h2@example.com",
                    "phoneNumber": "+1234567890",
                    "dateOfBirth": "1990-01-01",
                    "nationalId": "IT-H2-DUPE2",
                    "type": "INDIVIDUAL"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload2)
                .when()
                .post(API_BASE)
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("DUPLICATE_EMAIL"));
    }

    @Test
    @DisplayName("POST /customers/{id}/activate should change status")
    void shouldActivateCustomer() {
        // Create
        String payload = """
                {
                    "firstName": "Status",
                    "lastName": "Test",
                    "email": "status.it.h2@example.com",
                    "phoneNumber": "+1234567890",
                    "dateOfBirth": "1990-01-01",
                    "nationalId": "IT-H2-STATUS",
                    "type": "INDIVIDUAL"
                }
                """;

        String id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post(API_BASE)
                .then()
                .extract().path("id");

        // Activate
        given()
                .contentType(ContentType.JSON)
                .when()
                .post(API_BASE + "/" + id + "/activate")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        // Verify GET returns active
        given()
                .when()
                .get(API_BASE + "/" + id)
                .then()
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    @DisplayName("GET /customers should return paginated list")
    void shouldGetPaginatedList() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get(API_BASE)
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("page", equalTo(0))
                .body("size", equalTo(10));
    }

    @Test
    @DisplayName("GET /customers/{id} should return 404 for non-existent")
    void shouldReturn404ForNonExistent() {
        given()
                .when()
                .get(API_BASE + "/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /customers should validate required fields")
    void shouldValidateRequiredFields() {
        String invalidPayload = """
                {
                    "firstName": "",
                    "lastName": ""
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .post(API_BASE)
                .then()
                .statusCode(400);
    }
}
