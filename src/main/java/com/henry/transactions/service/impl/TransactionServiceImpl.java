package com.henry.transactions.service.impl;

import com.henry.transactions.dto.request.ListTransactionsRequest;
import com.henry.transactions.dto.request.ReconcileRequest;
import com.henry.transactions.dto.request.RecordTransactionRequest;
import com.henry.transactions.dto.response.*;
import com.henry.transactions.entity.*;
import com.henry.transactions.enums.*;
import com.henry.transactions.exception.*;
import com.henry.transactions.repository.*;
import com.henry.transactions.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final StatementRepository statementRepository;
    private final ReconciliationRepository reconciliationRepository;
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    public TransactionServiceImpl(TransactionRepository transactionRepository, WalletRepository walletRepository, LedgerEntryRepository ledgerEntryRepository, StatementRepository statementRepository, ReconciliationRepository reconciliationRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.statementRepository = statementRepository;
        this.reconciliationRepository = reconciliationRepository;
    }

    // Record Transaction
    // Called by payment-gateway via gRPC after capture or refund

    @Override
    @Transactional
    public RecordTransactionResponse recordTransaction(RecordTransactionRequest req) {
        log.info("Recording transaction: userId={} type={} amount={}",
                req.getUserId(), req.getType(), req.getAmount());

        TransactionType type = parseType(req.getType());

        // Create transaction record
        Transaction tx = Transaction.builder()
                .userId(req.getUserId())
                .type(type)
                .status(TransactionStatus.PENDING)
                .amount(req.getAmount())
                .currency(req.getCurrency() != null ? req.getCurrency() : "INR")
                .providerOrderId(req.getProviderOrderId())
                .providerPaymentId(req.getProviderPaymentId())
                .providerRefundId(req.getProviderRefundId())
                .referenceId(req.getReferenceId())
                .description(req.getDescription())
                .metadata(req.getMetadata())
                .build();

        tx = transactionRepository.save(tx);

        // Update wallet and create ledger entries
        long newBalance = updateWalletAndLedger(tx);

        // Mark transaction as completed
        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);

        log.info("Transaction recorded: id={} balance={}", tx.getId(), newBalance);

        return RecordTransactionResponse.builder()
                .transactionId(tx.getId().toString())
                .status(TransactionStatus.COMPLETED.name())
                .balance(newBalance)
                .message("Transaction recorded successfully")
                .build();
    }

    // Wallet Update + Ledger Entries
    // Double-entry bookkeeping:
    //   payment → DEBIT user wallet (money going out to pay)
    //   refund → CREDIT user wallet (money coming back)
    //   transfer → depends on a direction

    @Transactional
    protected long updateWalletAndLedger(Transaction tx) {
        // Get or create wallet — uses SELECT FOR UPDATE to prevent race conditions
        Wallet wallet = walletRepository.findByUserIdForUpdate(tx.getUserId())
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder()
                                .userId(tx.getUserId())
                                .balance(0L)
                                .currency(tx.getCurrency())
                                .build()
                ));

        long previousBalance = wallet.getBalance();
        long newBalance;
        EntryType entryType;
        String description;

        switch (tx.getType()) {
            case PAYMENT -> {
                // Payment: debit the wallet
                newBalance = previousBalance - tx.getAmount();
                entryType = EntryType.DEBIT;
                description = "Payment: " + (tx.getDescription() != null ? tx.getDescription() : tx.getProviderPaymentId());
            }
            case REFUND -> {
                // Refund: credit the wallet
                newBalance = previousBalance + tx.getAmount();
                entryType = EntryType.CREDIT;
                description = "Refund: " + (tx.getDescription() != null ? tx.getDescription() : tx.getProviderRefundId());
            }
            default -> {
                newBalance = previousBalance;
                entryType = EntryType.CREDIT;
                description = tx.getDescription() != null ? tx.getDescription() : "Transfer";
            }
        }

        // Update wallet balance
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // Create ledger entry
        LedgerEntry entry = LedgerEntry.builder()
                .transaction(tx)
                .userId(tx.getUserId())
                .type(entryType)
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .balance(newBalance)
                .description(description)
                .build();

        ledgerEntryRepository.save(entry);

        log.info("Wallet updated: userId={} previous={} new={} type={}",
                tx.getUserId(), previousBalance, newBalance, entryType);

        return newBalance;
    }

    // Get Balance
    @Override
    @Transactional(readOnly = true)
    public WalletResponse getBalance(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElse(Wallet.builder()
                        .userId(userId)
                        .balance(0L)
                        .currency("INR")
                        .build());

        return WalletResponse.builder()
                .userId(userId)
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }

    // Get Transaction

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        Transaction tx = transactionRepository.findById(UUID.fromString(transactionId))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        return toResponse(tx);
    }

    // List Transactions

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> listTransactions(String userId, ListTransactionsRequest req) {
        TransactionType type = req.getType() != null ? parseType(req.getType()) : null;
        TransactionStatus status = req.getStatus() != null ? parseStatus(req.getStatus()) : null;
        LocalDateTime start = req.getStartDate() != null
                ? LocalDate.parse(req.getStartDate(), DateTimeFormatter.ISO_DATE).atStartOfDay() : null;
        LocalDateTime end = req.getEndDate() != null
                ? LocalDate.parse(req.getEndDate(), DateTimeFormatter.ISO_DATE).atTime(LocalTime.MAX) : null;

        PageRequest pageRequest = PageRequest.of(req.getPage(), req.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Transaction> page = transactionRepository.findWithFilters(
                userId, type, status, start, end, pageRequest);

        List<TransactionResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PagedResponse.<TransactionResponse>builder()
                .data(responses)
                .total(page.getTotalElements())
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    // Get Statement

    @Override
    @Transactional
    public StatementResponse getStatement(String userId, int month, int year) {
        // Check if a cached statement exists
        var existing = statementRepository.findByUserIdAndMonthAndYear(userId, month, year);
        if (existing.isPresent()) {
            Statement stmt = existing.get();
            // Fetch transactions for this period
            LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            Page<Transaction> txPage = transactionRepository.findByUserIdAndDateRange(
                    userId, start, end, PageRequest.of(0, 1000));

            return buildStatementResponse(stmt, txPage.getContent());
        }

        // Generate a fresh statement
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusSeconds(1);

        // Get opening balance — balance before this month's start
        long openingBalance = getBalanceAt(userId, start.minusSeconds(1));

        // Calculate totals
        Long totalDebits = transactionRepository.sumPaymentsByUserAndDateRange(userId, start, end);
        Long totalCredits = transactionRepository.sumRefundsByUserAndDateRange(userId, start, end);
        Long txCount = transactionRepository.countByUserAndDateRange(userId, start, end);

        long closingBalance = openingBalance + (totalCredits != null ? totalCredits : 0)
                - (totalDebits != null ? totalDebits : 0);

        Statement stmt = Statement.builder()
                .userId(userId)
                .month(month)
                .year(year)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .totalCredits(totalCredits != null ? totalCredits : 0L)
                .totalDebits(totalDebits != null ? totalDebits : 0L)
                .transactionCount(txCount != null ? txCount.intValue() : 0)
                .generatedAt(LocalDateTime.now())
                .build();

        stmt = statementRepository.save(stmt);

        Page<Transaction> txPage = transactionRepository.findByUserIdAndDateRange(
                userId, start, end, PageRequest.of(0, 1000));

        return buildStatementResponse(stmt, txPage.getContent());
    }

    // Reconcile
    // Compare payment-gateway records vs. transaction records

    @Override
    @Transactional
    public ReconcileResponse reconcile(ReconcileRequest req) {
        log.info("Reconciling: providerPaymentId={}", req.getProviderPaymentId());

        // Find transaction by provider payment ID
        var txOpt = transactionRepository.findByProviderPaymentId(req.getProviderPaymentId());

        ReconciliationStatus status;
        String message;
        long actualAmount = 0;
        String notes;

        if (txOpt.isEmpty()) {
            // Transaction isn't found in our records
            status = ReconciliationStatus.MISSING;
            message = "Transaction not found in transaction-service records";
            notes = "Provider payment ID " + req.getProviderPaymentId() + " has no matching transaction";
        } else {
            Transaction tx = txOpt.get();
            actualAmount = tx.getAmount();

            if (actualAmount == req.getExpectedAmount()) {
                status = ReconciliationStatus.MATCHED;
                message = "Transaction amounts match";
                notes = "Expected: " + req.getExpectedAmount() + ", Actual: " + actualAmount;
            } else {
                status = ReconciliationStatus.MISMATCHED;
                message = "Amount mismatch detected";
                notes = "Expected: " + req.getExpectedAmount() + ", Actual: " + actualAmount
                        + ", Difference: " + (actualAmount - req.getExpectedAmount());
            }
        }

        // Save reconciliation record
        ReconciliationRecord record = ReconciliationRecord.builder()
                .providerPaymentId(req.getProviderPaymentId())
                .providerOrderId(req.getProviderOrderId())
                .expectedAmount(req.getExpectedAmount())
                .actualAmount(actualAmount)
                .status(status)
                .notes(notes)
                .reconciledAt(LocalDateTime.now())
                .build();

        reconciliationRepository.save(record);

        log.info("Reconciliation complete: status={} providerPaymentId={}",
                status, req.getProviderPaymentId());

        return ReconcileResponse.builder()
                .status(status.name())
                .message(message)
                .actualAmount(actualAmount)
                .notes(notes)
                .build();
    }

    // Helpers

    private long getBalanceAt(String userId, LocalDateTime at) {
        // Get the most recent ledger entry before the given time
        return ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(e -> e.getCreatedAt().isBefore(at))
                .findFirst()
                .map(LedgerEntry::getBalance)
                .orElse(0L);
    }

    private StatementResponse buildStatementResponse(Statement stmt, List<Transaction> transactions) {
        return StatementResponse.builder()
                .userId(stmt.getUserId())
                .month(stmt.getMonth())
                .year(stmt.getYear())
                .openingBalance(stmt.getOpeningBalance())
                .closingBalance(stmt.getClosingBalance())
                .totalCredits(stmt.getTotalCredits())
                .totalDebits(stmt.getTotalDebits())
                .transactionCount(stmt.getTransactionCount())
                .transactions(transactions.stream().map(this::toResponse).collect(Collectors.toList()))
                .build();
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .transactionId(tx.getId().toString())
                .userId(tx.getUserId())
                .type(tx.getType().name())
                .status(tx.getStatus().name())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .providerPaymentId(tx.getProviderPaymentId())
                .providerOrderId(tx.getProviderOrderId())
                .referenceId(tx.getReferenceId())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private TransactionType parseType(String type) {
        try {
            return TransactionType.valueOf(type.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new TransactionException("Invalid transaction type: " + type);
        }
    }

    private TransactionStatus parseStatus(String status) {
        try {
            return TransactionStatus.valueOf(status.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new TransactionException("Invalid transaction status: " + status);
        }
    }

}
