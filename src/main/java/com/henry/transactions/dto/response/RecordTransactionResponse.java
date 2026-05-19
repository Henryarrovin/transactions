package com.henry.transactions.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecordTransactionResponse {

    private String transactionId;
    private String status;
    private Long balance;
    private String message;

}
