package com.henry.transactions.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private String transactionId;
    private String userId;
    private String type;
    private String status;
    private Long amount;
    private String currency;
    private String providerPaymentId;
    private String providerOrderId;
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;

}
