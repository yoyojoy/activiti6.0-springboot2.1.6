package com.shengyecapital.process.dto;

import com.shengyecapital.process.enums.ProcessDecisionEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TaskCompleteMQDto {

    private String processInstanceId;

    private String businessId;

    private String tenantId;

    /**
     * 任务环节定义key
     */
    private String taskStepKey;

    /**
     * 该环节审批决策
     */
    private ProcessDecisionEnum decision;
}
