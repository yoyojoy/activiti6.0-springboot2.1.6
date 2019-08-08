package com.shengyecapital.process.dto.ao;

import com.shengyecapital.process.dto.BaseEnvVo;
import com.shengyecapital.process.enums.ProcessDecisionEnum;
import lombok.Data;

@Data
public class CompleteTaskAo extends BaseEnvVo {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 处理人ID
     */
    private String dealUserId;

    /**
     * 处理人ID
     */
    private String dealUserName;

    /**
     * 批注信息
     */
    private String comment;

    /**
     * 任务处理决策(默认直接走'通过')
     */
    private Integer decision = ProcessDecisionEnum.PASS.getStatus();

}
