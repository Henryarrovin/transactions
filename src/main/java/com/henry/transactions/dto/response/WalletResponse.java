package com.henry.transactions.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletResponse {

    private String userId;
    private Long balance;
    private String currency;

}
