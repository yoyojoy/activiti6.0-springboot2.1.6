package com.shengyecapital.process.dto.ao;

import com.shengyecapital.process.common.BasePageDto;
import lombok.Data;

@Data
public class ProcessUndoQueryListAo extends BasePageDto {

    /**
     * 流程名称
     */
    private String processDefinitionName;

    /**
     * 客户名称
     */
    private String customerName;

    private String startTime;

    private String endTime;

    /**
     * 商户ID
     */
    private String tenantId;

    /**
     * 处理的ID结合
     * 可以类似于以下id: USER1001, ROLE5214, CUSTOM9527
     */
    private String[] dealIds;
}
