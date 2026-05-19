package com.henry.transactions.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StatementResponse {

    private String userId;
    private Integer month;
    private Integer year;
    private Long openingBalance;
    private Long closingBalance;
    private Long totalCredits;
    private Long totalDebits;
    private Integer transactionCount;
    private List<TransactionResponse> transactions;

}
