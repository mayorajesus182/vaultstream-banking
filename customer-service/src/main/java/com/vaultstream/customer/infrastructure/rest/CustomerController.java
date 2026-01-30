package com.vaultstream.customer.infrastructure.rest;

import com.vaultstream.common.dto.PageResponse;
import com.vaultstream.customer.application.command.CreateCustomerCommand;
import com.vaultstream.customer.application.command.UpdateCustomerCommand;
import com.vaultstream.customer.application.dto.CustomerDto;
import com.vaultstream.customer.application.usecase.CustomerUseCase;
import com.vaultstream.customer.domain.model.CustomerStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.vaultstream.common.dto.ErrorResponse;

import java.net.URI;
import java.util.List;

/**
 * REST Controller for Customer operations.
 * 
 * This is the driving adapter in hexagonal architecture.
 * Secured with role-based access control via OIDC/Keycloak.
 */
@Slf4j
@Path("/api/v1/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Customer", description = "Customer management operations")
public class CustomerController {

        private final CustomerUseCase customerUseCase;

        @Inject
        public CustomerController(CustomerUseCase customerUseCase) {
                this.customerUseCase = customerUseCase;
        }

        // ========================================
        // Commands (Write Operations)
        // ========================================

        @POST
        @RolesAllowed("admin")
        @Operation(summary = "Create a new customer")
        @APIResponses({
                        @APIResponse(responseCode = "201", description = "Customer created successfully", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
                        @APIResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @APIResponse(responseCode = "409", description = "Duplicate email or national ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        public Response createCustomer(@Valid CreateCustomerCommand command) {
                log.info("POST /api/v1/customers - Creating customer: {}", command.getEmail());

                CustomerDto created = customerUseCase.createCustomer(command);

                return Response
                                .created(URI.create("/api/v1/customers/" + created.getId()))
                                .entity(created)
                                .build();
        }

        @PUT
        @Path("/{customerId}")
        @RolesAllowed({ "admin", "user" })
        @Operation(summary = "Update customer information")
        @APIResponses({
                        @APIResponse(responseCode = "200", description = "Customer updated successfully", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
                        @APIResponse(responseCode = "404", description = "Customer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @APIResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        public Response updateCustomer(
                        @Parameter(description = "Customer ID") @PathParam("customerId") String customerId,
                        @Valid UpdateCustomerCommand command) {

                log.info("PUT /api/v1/customers/{} - Updating customer", customerId);
                command.setCustomerId(customerId);

                CustomerDto updated = customerUseCase.updateCustomer(command);
                return Response.ok(updated).build();
        }

        @POST
        @Path("/{customerId}/activate")
        @RolesAllowed("admin")
        @Operation(summary = "Activate a customer")
        @APIResponses({
                        @APIResponse(responseCode = "200", description = "Customer activated", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
                        @APIResponse(responseCode = "404", description = "Customer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @APIResponse(responseCode = "400", description = "Invalid status transition", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        public Response activateCustomer(
                        @Parameter(description = "Customer ID") @PathParam("customerId") String customerId) {

                log.info("POST /api/v1/customers/{}/activate", customerId);

                CustomerDto activated = customerUseCase.activateCustomer(customerId);
                return Response.ok(activated).build();
        }

        @POST
        @Path("/{customerId}/suspend")
        @RolesAllowed("admin")
        @Operation(summary = "Suspend a customer")
        @APIResponses({
                        @APIResponse(responseCode = "200", description = "Customer suspended", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
                        @APIResponse(responseCode = "404", description = "Customer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @APIResponse(responseCode = "400", description = "Invalid status transition or missing reason", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        public Response suspendCustomer(
                        @Parameter(description = "Customer ID") @PathParam("customerId") String customerId,
                        @Parameter(description = "Reason for suspension", required = true) @QueryParam("reason") String reason) {

                log.info("POST /api/v1/customers/{}/suspend - Reason: {}", customerId, reason);

                CustomerDto suspended = customerUseCase.suspendCustomer(customerId, reason);
                return Response.ok(suspended).build();
        }

        @DELETE
        @Path("/{customerId}")
        @Operation(summary = "Deactivate a customer (soft delete)")
        @APIResponses({
                        @APIResponse(responseCode = "204", description = "Customer deactivated"),
                        @APIResponse(responseCode = "404", description = "Customer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @APIResponse(responseCode = "400", description = "Invalid status transition", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        public Response deactivateCustomer(
                        @Parameter(description = "Customer ID") @PathParam("customerId") String customerId) {

                log.info("DELETE /api/v1/customers/{}", customerId);

                customerUseCase.deactivateCustomer(customerId);
                return Response.noContent().build();
        }

        // ========================================
        // Queries (Read Operations)
        // ========================================

        @GET
        @Path("/{customerId}")
        @Operation(summary = "Get customer by ID")
        @APIResponses({
                        @APIResponse(responseCode = "200", description = "Customer found", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
                        @APIResponse(responseCode = "404", description = "Customer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        public Response getCustomerById(
                        @Parameter(description = "Customer ID") @PathParam("customerId") String customerId) {

                log.debug("GET /api/v1/customers/{}", customerId);

                CustomerDto customer = customerUseCase.getCustomerById(customerId);
                return Response.ok(customer).build();
        }

        @GET
        @Path("/number/{customerNumber}")
        @Operation(summary = "Get customer by customer number")
        @APIResponses({
                        @APIResponse(responseCode = "200", description = "Customer found", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
                        @APIResponse(responseCode = "404", description = "Customer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        public Response getCustomerByNumber(
                        @Parameter(description = "Customer number") @PathParam("customerNumber") String customerNumber) {

                log.debug("GET /api/v1/customers/number/{}", customerNumber);

                CustomerDto customer = customerUseCase.getCustomerByNumber(customerNumber);
                return Response.ok(customer).build();
        }

        @GET
        @Operation(summary = "Get all customers with pagination")
        @APIResponses({
                        @APIResponse(responseCode = "200", description = "List of customers", content = @Content(schema = @Schema(implementation = PageResponse.class)))
        })
        public Response getAllCustomers(
                        @Parameter(description = "Page number (0-based)") @QueryParam("page") @DefaultValue("0") int page,
                        @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("20") int size) {

                log.debug("GET /api/v1/customers - page: {}, size: {}", page, size);

                PageResponse<CustomerDto> customers = customerUseCase.getAllCustomers(page, size);
                return Response.ok(customers).build();
        }

        @GET
        @Path("/search")
        @Operation(summary = "Search customers by name")
        @APIResponses({
                        @APIResponse(responseCode = "200", description = "Search results", content = @Content(schema = @Schema(implementation = PageResponse.class)))
        })
        public Response searchByName(
                        @Parameter(description = "Name to search") @QueryParam("name") String name,
                        @Parameter(description = "Page number") @QueryParam("page") @DefaultValue("0") int page,
                        @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("20") int size) {

                log.debug("GET /api/v1/customers/search - name: {}", name);

                PageResponse<CustomerDto> results = customerUseCase.searchByName(name, page, size);
                return Response.ok(results).build();
        }

        @GET
        @Path("/status/{status}")
        @Operation(summary = "Get customers by status")
        @APIResponses({
                        @APIResponse(responseCode = "200", description = "List of customers with given status", content = @Content(schema = @Schema(implementation = CustomerDto.class, type = SchemaType.ARRAY)))
        })
        public Response getCustomersByStatus(
                        @Parameter(description = "Customer status", schema = @Schema(implementation = CustomerStatus.class)) @PathParam("status") CustomerStatus status) {

                log.debug("GET /api/v1/customers/status/{}", status);

                List<CustomerDto> customers = customerUseCase.getCustomersByStatus(status);
                return Response.ok(customers).build();
        }
}
