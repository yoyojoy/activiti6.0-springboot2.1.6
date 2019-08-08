package com.shengyecapital.process.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    long totalRecords;

    int totalPages;

    List<T> records;

    public PageResult() {
    }

    public PageResult(List<T> list, long totalRecords, int totalPages) {
        this.totalRecords = totalRecords;
        this.totalPages = totalPages;
        this.records = list;
    }
}
