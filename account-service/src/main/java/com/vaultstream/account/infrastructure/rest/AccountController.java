package com.vaultstream.account.infrastructure.rest;

import com.vaultstream.account.application.command.CreateAccountCommand;
import com.vaultstream.account.application.command.DepositMoneyCommand;
import com.vaultstream.account.application.command.WithdrawMoneyCommand;
import com.vaultstream.account.application.dto.AccountDto;
import com.vaultstream.account.application.service.AccountCommandHandler;
import com.vaultstream.account.application.service.AccountQueryHandler;
import com.vaultstream.common.dto.PageResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Account operations.
 * 
 * Implements CQRS by routing commands and queries to separate handlers.
 */
@Slf4j
@Path("/api/v1/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Accounts", description = "Account management with CQRS and Event Sourcing")
public class AccountController {

    private final AccountCommandHandler commandHandler;
    private final AccountQueryHandler queryHandler;

    @Inject
    public AccountController(AccountCommandHandler commandHandler, AccountQueryHandler queryHandler) {
        this.commandHandler = commandHandler;
        this.queryHandler = queryHandler;
    }

    // ========================================
    // Commands (Write Operations)
    // ========================================

    @POST
    @RolesAllowed("admin")
    @Operation(summary = "Create a new account")
    public Response createAccount(@Valid CreateAccountCommand command) {
        log.info("REST: Creating account for customer: {}", command.getCustomerId());
        AccountDto account = commandHandler.createAccount(command);
        return Response.status(Response.Status.CREATED).entity(account).build();
    }

    @POST
    @Path("/{id}/deposit")
    @RolesAllowed({"admin", "user"})
    @Operation(summary = "Deposit money into an account")
    public Response deposit(
            @PathParam("id") UUID accountId,
            @Valid DepositRequest request) {
        DepositMoneyCommand command = DepositMoneyCommand.builder()
                .accountId(accountId)
                .amount(request.amount())
                .description(request.description())
                .transactionReference(request.transactionReference())
                .build();

        AccountDto account = commandHandler.deposit(command);
        return Response.ok(account).build();
    }

    @POST
    @Path("/{id}/withdraw")
    @RolesAllowed({"admin", "user"})
    @Operation(summary = "Withdraw money from an account")
    public Response withdraw(
            @PathParam("id") UUID accountId,
            @Valid WithdrawRequest request) {
        WithdrawMoneyCommand command = WithdrawMoneyCommand.builder()
                .accountId(accountId)
                .amount(request.amount())
                .description(request.description())
                .transactionReference(request.transactionReference())
                .build();

        AccountDto account = commandHandler.withdraw(command);
        return Response.ok(account).build();
    }

    @POST
    @Path("/{id}/activate")
    @Operation(summary = "Activate an account")
    public Response activateAccount(@PathParam("id") UUID accountId) {
        AccountDto account = commandHandler.activateAccount(accountId);
        return Response.ok(account).build();
    }

    @POST
    @Path("/{id}/freeze")
    @Operation(summary = "Freeze an account")
    public Response freezeAccount(
            @PathParam("id") UUID accountId,
            FreezeRequest request) {
        AccountDto account = commandHandler.freezeAccount(accountId, request.reason());
        return Response.ok(account).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Close an account")
    public Response closeAccount(
            @PathParam("id") UUID accountId,
            CloseRequest request) {
        commandHandler.closeAccount(accountId, request != null ? request.reason() : "Account closed");
        return Response.noContent().build();
    }

    // ========================================
    // Queries (Read Operations)
    // ========================================

    @GET
    @Path("/{id}")
    @Operation(summary = "Get account by ID")
    public Response getAccountById(@PathParam("id") UUID accountId) {
        AccountDto account = queryHandler.getAccountById(accountId);
        return Response.ok(account).build();
    }

    @GET
    @Path("/number/{accountNumber}")
    @Operation(summary = "Get account by account number")
    public Response getAccountByNumber(@PathParam("accountNumber") String accountNumber) {
        AccountDto account = queryHandler.getAccountByNumber(accountNumber);
        return Response.ok(account).build();
    }

    @GET
    @Path("/customer/{customerId}")
    @Operation(summary = "Get all accounts for a customer")
    public Response getAccountsByCustomer(@PathParam("customerId") UUID customerId) {
        List<AccountDto> accounts = queryHandler.getAccountsByCustomerId(customerId);
        return Response.ok(accounts).build();
    }

    @GET
    @Operation(summary = "Get all accounts (paginated)")
    public Response getAllAccounts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PageResponse<AccountDto> response = queryHandler.getAllAccounts(page, size);
        return Response.ok(response).build();
    }

    // ========================================
    // Request DTOs
    // ========================================

    public record DepositRequest(
            java.math.BigDecimal amount,
            String description,
            String transactionReference) {}

    public record WithdrawRequest(
            java.math.BigDecimal amount,
            String description,
            String transactionReference) {}

    public record FreezeRequest(String reason) {}

    public record CloseRequest(String reason) {}
}
