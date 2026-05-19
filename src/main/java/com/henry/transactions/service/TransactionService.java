package com.henry.transactions.service;

import com.henry.transactions.dto.request.ListTransactionsRequest;
import com.henry.transactions.dto.request.ReconcileRequest;
import com.henry.transactions.dto.request.RecordTransactionRequest;
import com.henry.transactions.dto.response.*;

public interface TransactionService {

    RecordTransactionResponse recordTransaction(RecordTransactionRequest request);

    WalletResponse getBalance(String userId);

    TransactionResponse getTransaction(String transactionId);

    PagedResponse<TransactionResponse> listTransactions(String userId, ListTransactionsRequest request);

    StatementResponse getStatement(String userId, int month, int year);

    ReconcileResponse reconcile(ReconcileRequest request);

}
