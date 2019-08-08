package com.shengyecapital.process.dto.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class RuntimeInstanceListVo {

    /**
     * 业务对象名称
     */
    private String businessName;

    /**
     * 流程名称
     */
    private String processDefinitionName;

    /**
     * 当前任务名称
     */
    private String currentTaskName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date createTime;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

}
