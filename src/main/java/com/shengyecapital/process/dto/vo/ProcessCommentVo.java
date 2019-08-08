package com.shengyecapital.process.dto.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProcessCommentVo {

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务处理人
     */
    private String dealUserId;

    /**
     * 任务处理人
     */
    private String dealUserName;

    //任务处理时间
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date dealTime;

    /**
     * 处理意见
     */
    private String comment;

    /**
     * 操作名称
     */
    private String action;
}
