package com.shengyecapital.process.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "code",
        "message",
        "meta",
        "data"})
@Data
@NoArgsConstructor
public class CustomResponseBody<T> implements Serializable {

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("data")
    private T data;

    @Data
    private class Meta {

        @JsonProperty("totalSize")
        private Long totalRecords;

        @JsonProperty("totalPage")
        private Integer totalPages;

    }

    public CustomResponseBody(String code) {
        this.code = code;
    }

    public CustomResponseBody(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public CustomResponseBody(String code, T data) {
        this.code = code;
        this.data = data;
    }

    public CustomResponseBody(String code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public void setTotalRecords(long totalRecords) {
        meta.setTotalRecords(totalRecords);
    }

    public void setTotalPages(int totalPages) {
        meta.setTotalPages(totalPages);
    }

    public void setPageResult(PageResult<T> pageResult) {
        if (meta == null) {
            meta = new Meta();
        }

        meta.setTotalRecords(pageResult.getTotalRecords());
        meta.setTotalPages(pageResult.getTotalPages());

        setData((T) pageResult.getRecords());
    }

}
