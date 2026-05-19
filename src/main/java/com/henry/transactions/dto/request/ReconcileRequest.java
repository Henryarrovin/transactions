package com.henry.transactions.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReconcileRequest {

    @NotBlank(message = "providerPaymentId is required")
    private String providerPaymentId;

    private String providerOrderId;

    @NotNull(message = "expectedAmount is required")
    private Long expectedAmount;

}
