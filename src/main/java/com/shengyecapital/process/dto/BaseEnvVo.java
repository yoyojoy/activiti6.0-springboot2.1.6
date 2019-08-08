package com.shengyecapital.process.dto;

import lombok.Data;

@Data
public class BaseEnvVo {

    /**
     * 商户ID(工作流在商户间进行隔离)
     */
    private String tenantId;
}
