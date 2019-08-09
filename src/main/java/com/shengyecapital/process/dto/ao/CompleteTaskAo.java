package com.shengyecapital.process.dto.ao;

import com.shengyecapital.process.enums.ProcessDecisionEnum;
import lombok.Data;

@Data
public class CompleteTaskAo {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 处理ID
     */
    private String dealId;

    /**
     * 处理(人/角色/企业)名称
     */
    private String dealName;

    /**
     * 批注信息
     */
    private String comment;

    /**
     * 任务处理决策(默认直接走'通过')
     */
    private Integer decision = ProcessDecisionEnum.AGREE.getStatus();

}
