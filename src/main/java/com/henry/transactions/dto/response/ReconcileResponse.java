package com.henry.transactions.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReconcileResponse {

    private String status;
    private String message;
    private Long actualAmount;
    private String notes;

}
