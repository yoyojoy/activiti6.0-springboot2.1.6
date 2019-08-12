package com.shengyecapital.process.controller;

import com.shengyecapital.process.common.PageResult;
import com.shengyecapital.process.dto.ao.*;
import com.shengyecapital.process.dto.vo.*;
import com.shengyecapital.process.service.joy.ProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class ProcessManageController {

    @Autowired
    private ProcessService processService;

    /**
     * 部署流程 AMS-6
     * @param ao
     * @return
     * @throws Exception
     */
    @PostMapping("/process/deploy")
    public void deployProcess(DeploymentAo ao) {
        try{
            processService.deploy(ao);
        }catch (Exception e){
            log.error("流程部署失败, \n{}", e);
        }
    }

    /**
     * 查看已部署的流程定义列表 AMS-5
     * @param ao
     */
    @PostMapping("/process/definition/list")
    public PageResult<ProcessDeployedListVo> definitionList(DeployedProcessListQueryAo ao) {
        try{
            return  processService.getDeployedProcessList(ao);
        }catch (Exception e){
            log.error("查询流程部署列表失败, \n{}", e);
        }
        return null;
    }

    /**
     * 查看某个流程定义key下的历史版本列表 AMS-9
     * @param ao
     */
    @PostMapping("/process/deploy/list")
    public PageResult<ProcessDeployedListVo> historyProcessList(HistoryProcessListQueryAo ao) {
        try{
            return  processService.getHistoryProcessList(ao);
        }catch (Exception e){
            log.error("查询流程历史版本列表失败, \n{}", e);
        }
        return null;
    }

    /**
     * 删除部署的流程
     */
    @Deprecated
    @PostMapping("/process/deploy/remove")
    public void deploymentList(@RequestParam("deployId") String deployId, @RequestParam("tenantId") String tenantId) {
        try{
            processService.removeDeployedProcess(deployId, tenantId);
        }catch (Exception e){
            log.error("查询流程部署列表失败, \n{}", e);
        }
    }

    /**
     * 流程文件查看 AMS-7
     * @param processDefinitionId
     * @param resourceType {xml | image}
     * @param response
     */
    @GetMapping(value = "/process/file/view")
    public void getProcessXml(@RequestParam("processDefinitionId") String processDefinitionId,
                                                @RequestParam("resourceType") String resourceType,
                                                HttpServletResponse response) {
        processService.viewProcessDeployResource(processDefinitionId, resourceType, response);
    }

    /**
     * 流程文件导出
     * @param processDefinitionId
     * @param response
     */
    @GetMapping(value = "/process/file/export")
    public void exportProcessXml(@RequestParam("processDefinitionId") String processDefinitionId, HttpServletResponse response) {
        processService.exportProcessFile(processDefinitionId, response);
    }

    /**
     * 查看某个流程实例运行时高亮图
     * @param processInstanceId
     * @param response
     */
    @GetMapping(value = "/process/runtime/view")
    public void viewProcessRuntimeImage(@RequestParam("processInstanceId") String processInstanceId, HttpServletResponse response) {
        processService.viewProcessRuntimeImage(processInstanceId, response);
    }

    /**
     * 我已完成 的流程实例列表 AMS-2
     * @return
     */
    @PostMapping("/process/instance/unfinished/list")
    public PageResult<ProcessInstanceListVo> myCompleteProcesses(@RequestHeader("USER_ID") String userId, ProcessInstanceListQueryAo ao) {
        try {
            ao.setDealId(userId);
            return processService.personalCompleteProcessInstanceList(ao);
        }catch (Exception e){
            log.error("查询流程实例列表失败, \n{}", e);
        }
        return null;
    }

    /**
     * 流程实例列表 AMS-8 | AMS-3
     * @return
     */
    @PostMapping("/process/instance/list")
    public PageResult<ProcessInstanceListVo> runtimeInstancesList(ProcessInstanceListQueryAo ao) {
        try {
            return processService.getProcessInstanceList(ao);
        }catch (Exception e){
            log.error("查询流程实例列表失败, \n{}", e);
        }
        return null;
    }

    /**
     * 待办任务列表查询 AMS-1
     * @return
     */
    @PostMapping("/process/task/personal/list")
    public PageResult<ProcessUndoListVo> findPersonalTaskList(@RequestHeader("USER_ID") String userId, ProcessUndoQueryListAo ao) {
        try {
            List<String> ids = ao.getDealIds();
            if(CollectionUtils.isEmpty(ids)){
                ids = new ArrayList<>();
            }
            ids.add(userId);
            ao.setDealIds(ids);
            return processService.getPersonalUndoTaskList(ao);
        }catch (Exception e){
            log.error("查询流程待办列表失败, \n{}", e);
        }
        return null;
    }

    /**
     * 流程任务详情 AMS-4
     */
    @GetMapping("/process/task/detail/{taskId}")
    public ProcessTaskVo taskDetail(@PathVariable String taskId){
        return processService.getTaskDetail(taskId);
    }

    /**
     * 启动流程
     * @return
     */
    @PostMapping("/process/start")
    public void startProcess(@RequestBody ProcessStartAo ao) {
        try{
            processService.startProcess(ao);
        }catch (Exception e){
            log.error("发起流程失败, \n{}", e);
        }
    }

    /**
     * 获取流程实例批注
     * @param processInstanceId
     * @return
     */
    @GetMapping("/process/comment/list")
    public List<ProcessCommentVo> findProcessInstanceComments(@RequestParam("processInstanceId") String processInstanceId) {
        return processService.getProcessComments(processInstanceId);
    }

    /**
     * 任务处理 AMS-10
     */
    @PostMapping("/process/task/complete")
    public void taskProcess(@RequestBody CompleteTaskAo ao){
        processService.taskProcess(ao);
    }

}
