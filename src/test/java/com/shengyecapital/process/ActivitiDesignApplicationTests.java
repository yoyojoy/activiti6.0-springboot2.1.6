package com.shengyecapital.process;

import com.alibaba.fastjson.JSON;
import com.shengyecapital.process.dto.ao.CompleteTaskAo;
import com.shengyecapital.process.dto.ao.ProcessStartAo;
import com.shengyecapital.process.enums.ProcessDecisionEnum;
import com.shengyecapital.process.service.joy.ProcessService;
import com.shengyecapital.process.service.joy.ProcessUtil;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ActivitiDesignApplicationTests {

    @Autowired
    private ProcessService processService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessUtil processUtil;

    @Test
    public void test() {
        String taskId = "5f23f187-b80c-11e9-ac92-8cec4b5172d5";
        String tenantId = "pay-dev";
        System.out.println("\n=========================================\n");
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        FlowElement next = processUtil.getNextTaskElement(task, ProcessDecisionEnum.PASS);
        System.out.println(JSON.toJSONString(next));
    }

    @Test
    public void start(){
        ProcessStartAo ao = new ProcessStartAo();
        ao.setBusinessId("201910100001");
        ao.setProcessStarterId("U20190001");
        ao.setProcessStarterName("张三");
        ao.setCustomerId("C20191001");
        ao.setCustomerName("测试科技有限公司");
        ao.setTenantId("pay-dev");
        ao.setProcessDefinitionKey("test-process");
        processService.startProcess(ao);
    }

    @Test
    public void dealTask(){
        String  proceInsId = "cf4a16ab-b834-11e9-80c3-8cec4b5172d5";
        CompleteTaskAo ao = new CompleteTaskAo();
        ao.setTaskId("201910100001");
        ao.setDealUserId("gys_user001");
        ao.setDealUserName("供应商用户001");
        ao.setComment("供应商001批注");
        ao.setTenantId("pay-dev");
        ao.setDecision(ProcessDecisionEnum.PASS.getStatus());
        processService.taskProcess(ao);
    }

}
