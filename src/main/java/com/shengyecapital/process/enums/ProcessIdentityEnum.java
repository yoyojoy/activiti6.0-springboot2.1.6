package com.shengyecapital.process.enums;

/**
 * 流程参与身份定义
 */
public enum ProcessIdentityEnum {
    /*如下定义只是范例,需要以真实业务场景和发起流程而定*/

    //供应商
    GYS("GYS"),
    //项目公司
    XMGS("XMGS"),
    //债务人
    ZWR("ZWR"),
    //保理商
    BLS("BLS");

    private String key;

    ProcessIdentityEnum(String status) {
        this.key = status;
    }

    public String getKey() {
        return key;
    }
}
