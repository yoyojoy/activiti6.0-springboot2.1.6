package com.shengyecapital.process.dto.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProcessDeployedListVo {

    /**
     * 业务类别
     */
    private String businessType;

    /**
     * 流程名称
     */
    private String processDefinitionName;

    /**
     * 流程关键字
     */
    private String processDefinitionKey;

    /**
     * 流程部署时间
     */
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date deployTime;

    /**
     * 流程版本号
     */
    private Integer version;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程部署ID
     */
    private String deploymentId;

}
