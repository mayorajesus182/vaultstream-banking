package com.vaultstream.customer.infrastructure.rest;

import com.vaultstream.customer.application.command.CreateCustomerCommand;
import com.vaultstream.customer.domain.model.CustomerType;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * Integration tests for Customer REST API.
 * Tests run against a real Quarkus application with H2 database.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Customer REST API")
class CustomerControllerIT {

    private static String createdCustomerId;

    private CreateCustomerCommand createValidRequest() {
        CreateCustomerCommand request = new CreateCustomerCommand();
        request.setFirstName("Integration");
        request.setLastName("Test");
        request.setEmail("integration.test@example.com");
        request.setPhoneNumber("+1234567890");
        request.setDateOfBirth(LocalDate.of(1985, 3, 20));
        request.setNationalId("INT-TEST-001");
        request.setType(CustomerType.INDIVIDUAL);
        return request;
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/customers - Create customer returns 201")
    void createCustomer_shouldReturn201() {
        CreateCustomerCommand request = createValidRequest();

        createdCustomerId = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/customers")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("customerNumber", startsWith("CUST-"))
                .body("firstName", equalTo("Integration"))
                .body("lastName", equalTo("Test"))
                .body("email", equalTo("integration.test@example.com"))
                .body("status", equalTo("PENDING_VERIFICATION"))
                .extract()
                .path("id");
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/customers - Duplicate email returns 409")
    void createCustomer_duplicateEmail_shouldReturn409() {
        CreateCustomerCommand request = createValidRequest();
        // Same email as Order(1) test

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/customers")
                .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_EMAIL"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/customers/{id} - Get by ID returns 200")
    void getCustomerById_shouldReturn200() {
        given()
                .when()
                .get("/api/v1/customers/{id}", createdCustomerId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdCustomerId))
                .body("firstName", equalTo("Integration"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/customers/{id} - Not found returns 404")
    void getCustomerById_notFound_shouldReturn404() {
        String randomId = "00000000-0000-0000-0000-000000000000";

        given()
                .when()
                .get("/api/v1/customers/{id}", randomId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v1/customers - List with pagination returns 200")
    void getAllCustomers_shouldReturn200WithPagination() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/customers")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("page", equalTo(0))
                .body("size", equalTo(10))
                .body("totalElements", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/v1/customers/{id}/activate - Activate returns 200")
    void activateCustomer_shouldReturn200() {
        given()
                .when()
                .post("/api/v1/customers/{id}/activate", createdCustomerId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/v1/customers/{id}/suspend - Suspend returns 200")
    void suspendCustomer_shouldReturn200() {
        given()
                .queryParam("reason", "Test suspension")
                .when()
                .post("/api/v1/customers/{id}/suspend", createdCustomerId)
                .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"));
    }

    @Test
    @Order(8)
    @DisplayName("PUT /api/v1/customers/{id} - Update returns 200")
    void updateCustomer_shouldReturn200() {
        // First create a new customer for update test
        CreateCustomerCommand createRequest = new CreateCustomerCommand();
        createRequest.setFirstName("Update");
        createRequest.setLastName("Test");
        createRequest.setEmail("update.test@example.com");
        createRequest.setPhoneNumber("+9876543210");
        createRequest.setDateOfBirth(LocalDate.of(1980, 1, 1));
        createRequest.setNationalId("UPD-TEST-001");
        createRequest.setType(CustomerType.INDIVIDUAL);

        String customerId = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .when()
                .post("/api/v1/customers")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Now update
        String updateBody = """
                {
                    "firstName": "Updated",
                    "lastName": "Customer"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/v1/customers/{id}", customerId)
                .then()
                .statusCode(200)
                .body("firstName", equalTo("Updated"))
                .body("lastName", equalTo("Customer"));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/v1/customers/search - Search by name returns 200")
    void searchByName_shouldReturn200() {
        given()
                .queryParam("name", "Integration")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/customers/search")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/v1/customers/status/{status} - Get by status returns 200")
    void getCustomersByStatus_shouldReturn200() {
        given()
                .when()
                .get("/api/v1/customers/status/{status}", "SUSPENDED")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(100)
    @DisplayName("DELETE /api/v1/customers/{id} - Deactivate returns 204")
    void deactivateCustomer_shouldReturn204() {
        // Create a customer to delete
        CreateCustomerCommand request = new CreateCustomerCommand();
        request.setFirstName("Delete");
        request.setLastName("Test");
        request.setEmail("delete.test@example.com");
        request.setPhoneNumber("+1111111111");
        request.setDateOfBirth(LocalDate.of(1995, 6, 15));
        request.setNationalId("DEL-TEST-001");
        request.setType(CustomerType.INDIVIDUAL);

        String customerId = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/customers")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Delete (deactivate)
        given()
                .when()
                .delete("/api/v1/customers/{id}", customerId)
                .then()
                .statusCode(204);
    }
}
