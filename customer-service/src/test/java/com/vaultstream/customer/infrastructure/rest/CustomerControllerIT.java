package com.vaultstream.customer.infrastructure.rest;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.redis.client.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@DisplayName("Customer REST API Integration")
class CustomerControllerIT {

    private static final String API_BASE = "/api/v1/customers";

    @InjectMock
    RedisClient redisClient;

    @BeforeEach
    void setup() {
        // Mock Redis for Rate Limiting to always allow
        Response response = mock(Response.class);
        when(response.toLong()).thenReturn(1L);
        when(redisClient.incr(anyString())).thenReturn(response);

        // Mock expire
        when(redisClient.expire(anyString(), anyString())).thenReturn(response);

        // Mock get for Health Check (if needed)
        Response pong = mock(Response.class);
        when(pong.toString()).thenReturn("PONG");
        // But health check usually calls ping/echo.
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
}
