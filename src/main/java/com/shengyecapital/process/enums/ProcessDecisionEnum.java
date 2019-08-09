package com.shengyecapital.process.enums;

/**
 * 流程决策枚举
 */
public enum ProcessDecisionEnum {
    /**
     * 同意
     */
    AGREE(1),
    /**
     * 退回
     */
    RETURN(2),
    /**
     * 驳回
     */
    REJECT(3),
    /**
     * 终止
     */
    TERMINATE(4);

    private Integer status;

    ProcessDecisionEnum(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

    public static ProcessDecisionEnum from(Integer code) {
        for(ProcessDecisionEnum processDecisionEnum: ProcessDecisionEnum.values()){
            if(processDecisionEnum.getStatus().equals(code)){
                return processDecisionEnum;
            }
        }
        return null;
    }
}
