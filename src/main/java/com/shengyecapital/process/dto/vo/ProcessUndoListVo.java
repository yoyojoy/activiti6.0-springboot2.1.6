package com.shengyecapital.process.dto.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProcessUndoListVo {

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 流程名称
     */
    private String processName;

    /**
     * 流程发起人ID
     */
    private String processStarterId;

    /**
     * 流程发起人名称
     */
    private String processStarterName;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 流程发起时间
     */
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date processStartTime;

    /**
     * 任务生成时间
     */
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date taskStartTime;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 商户ID
     */
    private String tenantId;

}
