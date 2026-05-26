package com.henry.transactions.controller;

import com.henry.transactions.dto.request.ListTransactionsRequest;
import com.henry.transactions.dto.request.ReconcileRequest;
import com.henry.transactions.dto.request.RecordTransactionRequest;
import com.henry.transactions.dto.response.*;
import com.henry.transactions.grpc.AuthGrpcClient;
import com.henry.transactions.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthGrpcClient authGrpcClient;

    // Record Transaction

    @PostMapping
    public ResponseEntity<RecordTransactionResponse> recordTransaction(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody RecordTransactionRequest request) {

        validateToken(authHeader);
        log.info("POST /transactions userId={} type={}", request.getUserId(), request.getType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.recordTransaction(request));
    }

    // Get Balance

    @GetMapping("/balance/{userId}")
    public ResponseEntity<WalletResponse> getBalance(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userId) {

        var claims = validateToken(authHeader);
        // Users can only see their own balance; admins can see anyone's
        if (!claims.getRoles().contains("admin") && !claims.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("GET /transactions/balance/{}", userId);
        return ResponseEntity.ok(transactionService.getBalance(userId));
    }

    // Get Transaction

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String transactionId) {

        validateToken(authHeader);
        log.info("GET /transactions/{}", transactionId);
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }

    // List Transactions

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<TransactionResponse>> listTransactions(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        var claims = validateToken(authHeader);
        if (!claims.getRoles().contains("admin") && !claims.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var req = new ListTransactionsRequest();
        req.setType(type);
        req.setStatus(status);
        req.setStartDate(startDate);
        req.setEndDate(endDate);
        req.setPage(page);
        req.setPageSize(pageSize);

        log.info("GET /transactions/user/{}", userId);
        return ResponseEntity.ok(transactionService.listTransactions(userId, req));
    }

    // Get Statement

    @GetMapping("/statement/{userId}")
    public ResponseEntity<StatementResponse> getStatement(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userId,
            @RequestParam int month,
            @RequestParam int year) {

        var claims = validateToken(authHeader);
        if (!claims.getRoles().contains("admin") && !claims.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("GET /transactions/statement/{} month={} year={}", userId, month, year);
        return ResponseEntity.ok(transactionService.getStatement(userId, month, year));
    }

    // Reconcile — admin only

    @PostMapping("/reconcile")
    public ResponseEntity<ReconcileResponse> reconcile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ReconcileRequest request) {

        var claims = validateToken(authHeader);
        if (!claims.getRoles().contains("admin")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("POST /transactions/reconcile providerPaymentId={}", request.getProviderPaymentId());
        return ResponseEntity.ok(transactionService.reconcile(request));
    }

    // Health

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"ok\",\"service\":\"transaction-service\"}");
    }

    // Token validation helper

    private AuthGrpcClient.ValidateResult validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.henry.transactions.exception.TransactionException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        var result = authGrpcClient.validateToken(token);
        if (!result.isValid()) {
            throw new com.henry.transactions.exception.TransactionException("Invalid or expired token: " + result.getError());
        }
        return result;
    }
}
