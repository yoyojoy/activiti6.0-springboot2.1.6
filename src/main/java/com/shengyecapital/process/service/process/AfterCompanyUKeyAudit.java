package com.shengyecapital.process.service.process;

import com.shengyecapital.process.enums.ProcessDecisionEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 企业申请ukey审核后的业务逻辑处理
 */
@Slf4j
@Service
public class AfterCompanyUKeyAudit implements AfterTaskDealCallback {

    @Override
    public void execute(ProcessDecisionEnum decision, String businessId) {
        //处理工作流审核后,业务状态流转
        log.info("\n==================任务处理完成后的处理测试===================\n");
        log.info("决策是: {}, 业务ID: {}", decision.getStatus(), businessId);
        log.info("\n==================任务处理完成后的处理测试===================\n");
    }
}
