package com.henry.transactions.dto.request;

import lombok.Data;

@Data
public class ListTransactionsRequest {

    private String type;
    private String status;
    private String startDate;
    private String endDate;
    private int page = 0;
    private int pageSize = 20;

}
