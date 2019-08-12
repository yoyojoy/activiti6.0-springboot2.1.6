package com.shengyecapital.process.enums;

/**
 * 流程商户定义枚举
 */
public enum ProcessTenantEnum {
    /**
     * 资管ABS
     */
    ZGABS("zgabs", "资管ABS");

    private String tenantId;
    private String desc;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    ProcessTenantEnum(String tenantId, String des) {
        this.tenantId = tenantId;
        this.desc = des;
    }

    public static ProcessTenantEnum from(String tenantId) {
        for(ProcessTenantEnum Enum: ProcessTenantEnum.values()){
            if(Enum.getTenantId().equalsIgnoreCase(tenantId)){
                return Enum;
            }
        }
        return null;
    }
}
