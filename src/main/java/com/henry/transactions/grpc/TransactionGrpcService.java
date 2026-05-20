package com.henry.transactions.grpc;

import com.henry.transactions.dto.request.ListTransactionsRequest;
import com.henry.transactions.dto.request.ReconcileRequest;
import com.henry.transactions.dto.request.RecordTransactionRequest;
import com.henry.transactions.dto.response.PagedResponse;
import com.henry.transactions.dto.response.StatementResponse;
import com.henry.transactions.dto.response.TransactionResponse;
import com.henry.transaction.grpc.proto.*;
import com.henry.transactions.service.TransactionService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class TransactionGrpcService extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final TransactionService transactionService;

    // ── RecordTransaction
    // Called by payment-gateway after capture or refund

//    @Override
    public void recordTransaction(
            RecordTransactionRequest request,
            StreamObserver<RecordTransactionResponse> responseObserver) {

        log.info("gRPC RecordTransaction: userId={} type={} amount={}",
                request.getUserId(), request.getType(), request.getAmount());

        try {
            var req = new RecordTransactionRequest();
            req.setUserId(request.getUserId());
            req.setType(request.getType());
            req.setAmount(request.getAmount());
            req.setCurrency(request.getCurrency().isEmpty() ? "INR" : request.getCurrency());
            req.setProviderOrderId(request.getProviderOrderId());
            req.setProviderPaymentId(request.getProviderPaymentId());
            req.setProviderRefundId(request.getProviderRefundId());
            req.setReferenceId(request.getReferenceId());
            req.setDescription(request.getDescription());
            req.setMetadata(request.getMetadata());

            var result = transactionService.recordTransaction(req);

            responseObserver.onNext(RecordTransactionResponse.newBuilder()
                    .setTransactionId(result.getTransactionId())
                    .setStatus(result.getStatus())
                    .setBalance(result.getBalance())
                    .setMessage(result.getMessage())
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC RecordTransaction failed", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // GetBalance

    @Override
    public void getBalance(
            GetBalanceRequest request,
            StreamObserver<GetBalanceResponse> responseObserver) {

        log.info("gRPC GetBalance: userId={}", request.getUserId());

        try {
            var result = transactionService.getBalance(request.getUserId());

            responseObserver.onNext(GetBalanceResponse.newBuilder()
                    .setUserId(result.getUserId())
                    .setBalance(result.getBalance())
                    .setCurrency(result.getCurrency())
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC GetBalance failed", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // GetTransaction

    @Override
    public void getTransaction(
            GetTransactionRequest request,
            StreamObserver<GetTransactionResponse> responseObserver) {

        log.info("gRPC GetTransaction: transactionId={}", request.getTransactionId());

        try {
            var result = transactionService.getTransaction(request.getTransactionId());

            responseObserver.onNext(toProto(result));
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC GetTransaction failed", e);
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // ListTransactions

    @Override
    public void listTransactions(
            com.henry.transaction.grpc.proto.ListTransactionsRequest request,
            StreamObserver<ListTransactionsResponse> responseObserver) {

        log.info("gRPC ListTransactions: userId={}", request.getUserId());

        try {
            var req = new ListTransactionsRequest();
            req.setType(request.getType().isEmpty() ? null : request.getType());
            req.setStatus(request.getStatus().isEmpty() ? null : request.getStatus());
            req.setStartDate(request.getStartDate().isEmpty() ? null : request.getStartDate());
            req.setEndDate(request.getEndDate().isEmpty() ? null : request.getEndDate());
            req.setPage(request.getPage());
            req.setPageSize(request.getPageSize() == 0 ? 20 : request.getPageSize());

            PagedResponse<TransactionResponse> result =
                    transactionService.listTransactions(request.getUserId(), req);

            var builder = ListTransactionsResponse.newBuilder()
                    .setTotal(result.getTotal())
                    .setPage(result.getPage())
                    .setPageSize(result.getPageSize());

            result.getData().forEach(tx -> builder.addTransactions(toProto(tx)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC ListTransactions failed", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // GetStatement

    @Override
    public void getStatement(
            GetStatementRequest request,
            StreamObserver<GetStatementResponse> responseObserver) {

        log.info("gRPC GetStatement: userId={} month={} year={}",
                request.getUserId(), request.getMonth(), request.getYear());

        try {
            StatementResponse result = transactionService.getStatement(
                    request.getUserId(), request.getMonth(), request.getYear());

            var builder = GetStatementResponse.newBuilder()
                    .setUserId(result.getUserId())
                    .setMonth(result.getMonth())
                    .setYear(result.getYear())
                    .setOpeningBalance(result.getOpeningBalance())
                    .setClosingBalance(result.getClosingBalance())
                    .setTotalCredits(result.getTotalCredits())
                    .setTotalDebits(result.getTotalDebits())
                    .setTransactionCount(result.getTransactionCount());

            result.getTransactions().forEach(tx -> builder.addTransactions(toProto(tx)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC GetStatement failed", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // Reconcile

    @Override
    public void reconcile(
            com.henry.transaction.grpc.proto.ReconcileRequest request,
            StreamObserver<ReconcileResponse> responseObserver) {

        log.info("gRPC Reconcile: providerPaymentId={}", request.getProviderPaymentId());

        try {
            var req = new ReconcileRequest();
            req.setProviderPaymentId(request.getProviderPaymentId());
            req.setProviderOrderId(request.getProviderOrderId());
            req.setExpectedAmount(request.getExpectedAmount());

            var result = transactionService.reconcile(req);

            responseObserver.onNext(ReconcileResponse.newBuilder()
                    .setStatus(result.getStatus())
                    .setMessage(result.getMessage())
                    .setActualAmount(result.getActualAmount())
                    .setNotes(result.getNotes() != null ? result.getNotes() : "")
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC Reconcile failed", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // Helper

    private GetTransactionResponse toProto(TransactionResponse tx) {
        return GetTransactionResponse.newBuilder()
                .setTransactionId(tx.getTransactionId())
                .setUserId(tx.getUserId())
                .setType(tx.getType())
                .setStatus(tx.getStatus())
                .setAmount(tx.getAmount())
                .setCurrency(tx.getCurrency())
                .setProviderPaymentId(tx.getProviderPaymentId() != null ? tx.getProviderPaymentId() : "")
                .setDescription(tx.getDescription() != null ? tx.getDescription() : "")
                .setCreatedAt(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "")
                .build();
    }
}

