package com.shengyecapital.process.dto.ao;

import com.shengyecapital.process.common.BasePageDto;
import lombok.Data;

/**
 * 流程实例列表查询条件
 */
@Data
public class ProcessInstanceListQueryAo extends BasePageDto {

    /**
     * 流程名称
     */
    private String processDefinitionName;

    /**
     * 业务对象名称
     */
    private String businessName;

    private String startTime;

    private String endTime;

    /**
     * 当前任务名称
     */
    private String currentTaskName;

    /**
     * 商户ID
     */
    private String tenantId;

    /**
     * 是否完结
     */
    private Boolean isFinished = false;

    /**
     * 处理人 AMS-2
     */
    private String dealId;

}
