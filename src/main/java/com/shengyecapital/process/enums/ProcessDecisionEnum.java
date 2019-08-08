package com.shengyecapital.process.enums;

/**
 * 流程决策枚举
 */
public enum ProcessDecisionEnum {
    /**
     * 通过
     */
    PASS(1),
    /**
     * 退回
     */
    RETURN(2),
    /**
     * 拒绝
     */
    REJECT(3);

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
