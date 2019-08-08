package com.shengyecapital.process.dto;

import lombok.Data;

@Data
public class ProcessParam {

    /**
     * 对应流程图中每个环节的定义ID (约定key)
     */
    private String key;

    /**
     * 当对应环节key的处理人是动态指定时
     * 需要指定该字段的值
     * 用户以"USER"开头, 例: USER1001
     * 角色以"ROLE"开头, 例: ROLE9365
     * 企业以"CUSTOM"开头, 例: CUSTOM9527
     */
    private String[] assigneeList;

}
