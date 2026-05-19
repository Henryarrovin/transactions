package com.henry.transactions.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordTransactionRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "type is required")
    private String type; // payment / refund / transfer

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be greater than 0")
    private Long amount; // in paise

    private String currency = "INR";
    private String providerOrderId;
    private String providerPaymentId;
    private String providerRefundId;
    private String referenceId;
    private String description;
    private String metadata;

}
