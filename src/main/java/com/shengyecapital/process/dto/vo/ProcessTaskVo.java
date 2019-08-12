package com.shengyecapital.process.dto.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProcessTaskVo {

    /**
     * 当前任务名称
     */
    private String taskName;

    /**
     * 流程发起人
     */
    private String processStarterName;

    //当前任务开始时间
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date taskStartTime;

    //流程发起时间
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date processStartTime;

    /**
     * 当前任务执行(个人/角色/企业)
     */
    private String dealer;

    /**
     * 操作
     */
    private String action;

    /**
     * 绑定的业务唯一标识
     */
    private String businessId;

    /**
     * 商户ID
     */
    private String tenantId;

}
