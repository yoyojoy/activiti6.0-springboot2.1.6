package com.shengyecapital.process.dto;

import lombok.Data;

@Data
public class TaskCompleteMQDto {

    private String processInstanceId;

    private String businessId;

    private String tenantId;

    public TaskCompleteMQDto(String processInstanceId, String businessId, String tenantId) {
        this.processInstanceId = processInstanceId;
        this.businessId = businessId;
        this.tenantId = tenantId;
    }
}
