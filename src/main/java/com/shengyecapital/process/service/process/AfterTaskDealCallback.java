package com.shengyecapital.process.service.process;

import com.shengyecapital.process.enums.ProcessDecisionEnum;

//任务处理完成后的回调接口
public interface AfterTaskDealCallback {

    void execute(ProcessDecisionEnum decision, String businessId);
}
