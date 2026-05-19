package com.henry.transactions.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PagedResponse<T> {

    private List<T> data;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

}
