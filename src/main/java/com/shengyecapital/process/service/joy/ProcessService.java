package com.shengyecapital.process.service.joy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.shengyecapital.process.common.PageResult;
import com.shengyecapital.process.constant.ProcessConstant;
import com.shengyecapital.process.dto.ProcessParam;
import com.shengyecapital.process.dto.TaskCompleteMQDto;
import com.shengyecapital.process.dto.ao.*;
import com.shengyecapital.process.dto.vo.*;
import com.shengyecapital.process.enums.ProcessDecisionEnum;
import com.shengyecapital.process.exception.ServerErrorException;
import com.shengyecapital.process.mapper.ActivitiMapper;
import com.shengyecapital.process.service.process.AfterTaskDealCallback;
import com.shengyecapital.process.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.CommentEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProcessService {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private ActivitiMapper activitiMapper;
    @Autowired
    private ProcessUtil processUtil;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 热部署流程
     *
     * @param ao
     * @throws Exception
     */
    public void deploy(DeploymentAo ao) throws Exception {
        if (StringUtils.isBlank(ao.getTenantId())) {
            throw new ServerErrorException("商户标识tenantId不能为空");
        }
        if (ao.getFile() == null) {
            throw new ServerErrorException("流程文件不能为空");
        }
        if (StringUtils.isBlank(ao.getProcessName())) {
            throw new ServerErrorException("流程名称name不能为空");
        }
        MultipartFile file = ao.getFile();
        String fileName = file.getOriginalFilename();
        InputStream in = file.getInputStream();
        String xml = IOUtils.toString(in, "UTF-8");
        Document document = DocumentHelper.parseText(xml);
        Element root = document.getRootElement();
        Element process = root.element("process");
        if (process == null) {
            throw new ServerErrorException("流程配置文件有错误, 没有定义流程");
        }
        String processDefinitionKey = process.attribute("id").getValue();
        if (StringUtils.isNotBlank(ao.getProcessKey()) && !ao.getProcessKey().equalsIgnoreCase(processDefinitionKey)) {
            throw new ServerErrorException("输入的流程关键字和流程文件中定义的关键字不一致");
        }
        if (StringUtils.isBlank(processDefinitionKey)) {
            throw new ServerErrorException("流程定义id不能为空");
        }
        Deployment deployment = repositoryService.createDeployment().name(ao.getProcessName()).addString(fileName, xml)
                .tenantId(ao.getTenantId()).category(ao.getBusinessName()).key(processDefinitionKey.toLowerCase()).deploy();
        if (deployment == null) {
            throw new ServerErrorException("流程部署失败,请检查流程bpmn文件");
        }
        //该定义KEY的流程有部署过
        List<Model> olds = repositoryService.createModelQuery().modelKey(processDefinitionKey.toLowerCase()).
                modelTenantId(ao.getTenantId()).orderByModelVersion().desc().list();
        Model model = repositoryService.newModel();
        if (!CollectionUtils.isEmpty(olds)) {
            model.setVersion(olds.get(0).getVersion() + 1);
        } else {
            model.setVersion(1);
        }
        model.setName(ao.getProcessName());
        model.setKey(processDefinitionKey);
        model.setCategory(ao.getBusinessName());
        model.setDeploymentId(deployment.getId());
        model.setTenantId(ao.getTenantId());
        repositoryService.saveModel(model);
        log.info("商户{}部署新的流程成功, 流程定义ID: {}", ao.getTenantId(), deployment.getId());

    }

    private Map<String, Object> generateStartVariables(ProcessStartAo ao) {
        Map<String, Object> vars = new HashMap<>();
        if (!CollectionUtils.isEmpty(ao.getVariables())) {
            vars.put(ProcessConstant.PROCESS_PARAM, JSON.toJSONString(ao.getVariables()));
        }
        //流程的发起人ID
        vars.put(ProcessConstant.PROCESS_STARTER_ID, ao.getProcessStarterId());
        //流程的发起人姓名
        vars.put(ProcessConstant.PROCESS_STARTER_NAME, ao.getProcessStarterName());
        //客户名称
        vars.put(ProcessConstant.CUSTOMER_NAME, ao.getCustomerName());
        //客户ID
        vars.put(ProcessConstant.CUSTOMER_ID, ao.getCustomerId());
        //业务名称
        vars.put(ProcessConstant.BUSINESS_NAME, ao.getBusinessName());
        return vars;
    }

    /**
     * 发起流程
     *
     * @param ao
     */
    public void startProcess(ProcessStartAo ao) {
        if (StringUtils.isBlank(ao.getBusinessId())) {
            throw new ServerErrorException("业务唯一标识businessId不能为空");
        }
        if (StringUtils.isBlank(ao.getProcessStarterId())) {
            throw new ServerErrorException("发起人processStarterId不能为空");
        }
        if (StringUtils.isBlank(ao.getProcessDefinitionKey())) {
            throw new ServerErrorException("流程唯一标识processDefinitionKey不能为空");
        }
        if (StringUtils.isBlank(ao.getTenantId())) {
            throw new ServerErrorException("商户标识tenantId不能为空");
        }
        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(ao.getProcessDefinitionKey()).processInstanceBusinessKey(ao.getBusinessId())
                .processInstanceTenantId(ao.getTenantId()).list();
        if (!CollectionUtils.isEmpty(historicProcessInstances)) {
            throw new ServerErrorException("业务唯一标识businessId已有在途的流程业务");
        }
        ProcessInstance pi = runtimeService.startProcessInstanceByKeyAndTenantId(ao.getProcessDefinitionKey(), ao.getTenantId());
        if (pi == null) {
            throw new ServerErrorException("发起流程失败");
        }
        runtimeService.updateBusinessKey(pi.getProcessInstanceId(), ao.getBusinessId());
        //指发起的环节(one)
        Task task = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId())
                .processInstanceBusinessKey(ao.getBusinessId()).taskTenantId(ao.getTenantId()).active().singleResult();
        if (task == null) {
            throw new ServerErrorException("发起流程失败");
        }
        log.info("流程发起成功, 流程实例ID: {}", pi.getId());
        runtimeService.setVariables(task.getExecutionId(), this.generateStartVariables(ao));
        //完成第一个环节,并设置下一个环节的处理人
        this.dealTheFirstTask(ao.getTenantId(), task.getId(), ao.getProcessStarterId(), ao.getProcessStarterName());
        log.info("\n======================流程第一个环节已完成========================\n");
    }

    /*完成第一个环节(发起申请),并自动添加批注,并指定下一环节的审批人*/
    private void dealTheFirstTask(String tenantId, String taskId, String userId, String userName) {
        CompleteTaskAo ao = new CompleteTaskAo();
        ao.setDealId(userId);
        ao.setTenantId(tenantId);
        ao.setDealName(userName);
        ao.setComment("发起流程,提交申请");
        ao.setTaskId(taskId);
        ao.setDecision(ProcessDecisionEnum.AGREE.getStatus());
        this.taskProcess(ao, null);
    }

    /**
     * 查询已部署流程列表
     *
     * @param ao
     * @return
     */
    public PageResult<ProcessDeployedListVo> getDeployedProcessList(DeployedProcessListQueryAo ao) {
        Page<ProcessDeployedListVo> page = PageHelper.startPage(ao.getPageNum(), ao.getPageSize());
        StringBuffer sql = new StringBuffer("SELECT m.id_ processDefinitionId, m.version_ version, t.DEPLOY_TIME_ deployTime, m.name_ processDefinitionName, \n" +
                "\t\t\tm.key_ processDefinitionKey, t.category_ businessType, t.ID_ deploymentId, t.TENANT_ID_ tenantId  from (select a.* from ACT_RE_PROCDEF a RIGHT JOIN \n" +
                "\t(select MAX(VERSION_) version, KEY_ processDefKey from ACT_RE_PROCDEF GROUP BY KEY_) b\n" +
                "\ton a.VERSION_=b.version and a.KEY_=b.processDefKey) m LEFT JOIN ACT_RE_DEPLOYMENT t on m.DEPLOYMENT_ID_=t.ID_\n" +
                "\tLEFT JOIN ACT_RE_MODEL n on n.DEPLOYMENT_ID_=t.ID_ where 1=1 ");
        if (StringUtils.isNotBlank(ao.getProcessDefinitionKey())) {
            sql.append("and m.KEY_ like concat(%,").append(ao.getProcessDefinitionKey()).append(" %) ");
        }
        if (StringUtils.isNotBlank(ao.getProcessDefinitionName())) {
            sql.append("and m.NAME_ like concat(%,").append(ao.getProcessDefinitionName()).append(" %) ");
        }
        if (StringUtils.isNotBlank(ao.getBusinessType())) {
            sql.append("and t.CATEGORY_ like concat(%,").append(ao.getBusinessType()).append(" %) ");
        }
        if (StringUtils.isNotBlank(ao.getStartTime())) {
            sql.append("and t.DEPLOY_TIME_ >= DATE_FORMT(").append(ao.getStartTime()).append(", '%Y-%m-%d') ");
        }
        if (StringUtils.isNotBlank(ao.getEndTime())) {
            sql.append("and t.DEPLOY_TIME_ <= DATE_FORMT(").append(ao.getEndTime()).append(", '%Y-%m-%d') ");
        }
        if(StringUtils.isNotBlank(ao.getTenantId())){
            sql.append("and t.TENANT_ID_ ='").append(ao.getTenantId()).append("' ");
        }
        sql.append(" order by t.DEPLOY_TIME_ DESC ");
        List<ProcessDeployedListVo> data = activitiMapper.queryDeployedProcessesList(sql.toString());
        PageResult<ProcessDeployedListVo> pageResult = new PageResult<>();
        pageResult.setRecords(data);
        pageResult.setTotalPages(page.getPages());
        pageResult.setTotalRecords(page.getTotal());
        return pageResult;
    }

    /**
     * 查询某流程定义key的所有历史版本列表
     *
     * @return
     */
    public PageResult<ProcessDeployedListVo> getHistoryProcessList(HistoryProcessListQueryAo ao) {
        if (StringUtils.isBlank(ao.getTenantId())) {
            throw new ServerErrorException("商户标识tenantId不能为空");
        }
        List<Deployment> list = repositoryService.createDeploymentQuery().processDefinitionKey(ao.getProcessDefinitionKey())
                .deploymentTenantId(ao.getTenantId()).deploymentTenantId(ao.getTenantId()).list();
        List<ProcessDeployedListVo> res = new ArrayList<>();
        for (Deployment deployment : list) {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId())
                    .processDefinitionTenantId(ao.getTenantId()).active().singleResult();
            Model model = repositoryService.createModelQuery().deploymentId(deployment.getId()).modelTenantId(ao.getTenantId()).singleResult();
            ProcessDeployedListVo vo = new ProcessDeployedListVo();
            vo.setProcessDefinitionId(definition.getId());
            vo.setVersion(model.getVersion());
            vo.setProcessDefinitionKey(definition.getKey());
            vo.setDeployTime(deployment.getDeploymentTime());
            vo.setBusinessType(deployment.getCategory());
            vo.setDeploymentId(deployment.getId());
            vo.setProcessDefinitionName(definition.getName());
            vo.setTenantId(definition.getTenantId());
            res.add(vo);
        }
        return this.generatePageResult(res, ao.getPageNum(), ao.getPageSize());
    }

    public void removeDeployedProcess(String deployId, String tenantId) {
        //这里是否有需要进行级联的删除,包含流程定义的历史流程信息
        Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deployId).singleResult();
        if (deployment == null || !deployment.getTenantId().equalsIgnoreCase(tenantId)) {
            throw new ServerErrorException("流程部署不存在");
        }
        repositoryService.deleteDeployment(deployId);
    }

    /**
     * 待办任务列表查询
     *
     * @param ao
     * @return
     */
    public PageResult<ProcessUndoListVo> getUndoProcessList(ProcessUndoQueryListAo ao) throws Exception {
        List<ProcessUndoListVo> result = new ArrayList<>();
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
        if (StringUtils.isNotBlank(ao.getStartTime())) {
            query.finishedAfter(DateTimeUtil.parseToDate(ao.getStartTime(), DateTimeUtil.FMT_yyyyMMdd));
        }
        if (StringUtils.isNotBlank(ao.getEndTime())) {
            query.finishedBefore(DateTimeUtil.parseToDate(ao.getEndTime(), DateTimeUtil.FMT_yyyyMMdd));
        }
        if (StringUtils.isNotEmpty(ao.getCustomerName())) {
            query.variableValueEquals(ProcessConstant.CUSTOMER_NAME, ao.getCustomerName());
        }
        if (StringUtils.isNotBlank(ao.getProcessDefinitionName())) {
            query.processDefinitionName(ao.getProcessDefinitionName());
        }
        List<HistoricProcessInstance> processInstances = query.processInstanceTenantId(ao.getTenantId()).unfinished().includeProcessVariables().orderByProcessInstanceStartTime().asc().list();
        if (!CollectionUtils.isEmpty(processInstances)) {
            result = processInstances.stream().map(processInstance -> {
                Map<String, Object> map = processInstance.getProcessVariables();
                String processStarterId = map.get(ProcessConstant.PROCESS_STARTER_ID).toString();
                String processStarterName = map.get(ProcessConstant.PROCESS_STARTER_NAME).toString();
                String customerName = map.get(ProcessConstant.CUSTOMER_NAME).toString();
                List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskCreateTime().desc().active().list();
                for (Task task : tasks) {
                    ProcessUndoListVo target = new ProcessUndoListVo();
                    target.setProcessName(processInstance.getProcessDefinitionName());
                    target.setBusinessId(processInstance.getBusinessKey());
                    target.setProcessStarterId(processStarterId);
                    target.setProcessStarterName(processStarterName);
                    target.setCustomerName(customerName);
                    target.setTaskName(task.getName());
                    target.setProcessStartTime(processInstance.getStartTime());
                    target.setTaskId(task.getId());
                    return target;
                }
                return null;
            }).collect(Collectors.toList());
        }
        return this.generatePageResult(result, ao.getPageNum(), ao.getPageSize());
    }

    /**
     * 个人任务列表查询
     *
     * @param ao
     * @return
     */
    public PageResult<ProcessUndoListVo> getPersonalUndoTaskList(ProcessUndoQueryListAo ao) throws Exception {
        List<ProcessUndoListVo> result = new ArrayList<>();
        TaskQuery query = taskService.createTaskQuery();
        if (StringUtils.isNotBlank(ao.getStartTime())) {
            query.taskCreatedAfter(DateTimeUtil.parseToDate(ao.getStartTime(), DateTimeUtil.FMT_yyyyMMdd));
        }
        if (StringUtils.isNotBlank(ao.getEndTime())) {
            query.taskCreatedBefore(DateTimeUtil.parseToDate(ao.getEndTime(), DateTimeUtil.FMT_yyyyMMdd));
        }
        if (StringUtils.isNotEmpty(ao.getCustomerName())) {
            query.processVariableValueLikeIgnoreCase(ProcessConstant.CUSTOMER_NAME, ao.getCustomerName());
        }
        if (StringUtils.isNotBlank(ao.getProcessDefinitionName())) {
            query.processDefinitionNameLike(ao.getProcessDefinitionName());
        }
        if (StringUtils.isNotBlank(ao.getTenantId())) {
            query.taskTenantId(ao.getTenantId());
        }
        query.taskAssigneeLikeIgnoreCase(ao.getDealIds().get(0));
        List<Task> tasks = query.active().includeProcessVariables().orderByTaskCreateTime().desc().list();
        if (!CollectionUtils.isEmpty(tasks)) {
            for (Task task : tasks) {
                ProcessUndoListVo target = new ProcessUndoListVo();
                ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
                ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                Map<String, Object> map = task.getProcessVariables();
                String processStarterId = map.get(ProcessConstant.PROCESS_STARTER_ID).toString();
                String processStarterName = map.get(ProcessConstant.PROCESS_STARTER_NAME).toString();
                String customerName = map.get(ProcessConstant.CUSTOMER_NAME).toString();
                target.setProcessName(definition.getName());
                target.setBusinessId(instance.getBusinessKey());
                target.setProcessStarterId(processStarterId);
                target.setProcessStarterName(processStarterName);
                target.setCustomerName(customerName);
                target.setProcessStartTime(task.getCreateTime());
                target.setTaskName(task.getName());
                target.setTaskId(task.getId());
                target.setTaskStartTime(task.getCreateTime());
                target.setProcessInstanceId(task.getProcessInstanceId());
                target.setTenantId(task.getTenantId());
                result.add(target);
            }
        }
        return this.generatePageResult(result, ao.getPageNum(), ao.getPageSize());
    }

    /**
     * 查看流程文件
     *
     * @param definitionId
     * @return
     */
    public void viewProcessDeployResource(String definitionId, String resourceType, HttpServletResponse response) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();
        if (processDefinition == null) {
            throw new ServerErrorException("流程定义不存在");
        }
        String resourceName = "";
        if (resourceType.equals("image")) {
            resourceName = processDefinition.getDiagramResourceName();
        } else if (resourceType.equals("xml")) {
            resourceName = processDefinition.getResourceName();
        }
        InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), resourceName);
        try {
            IOUtils.copy(resourceAsStream, response.getOutputStream());
        } catch (IOException e) {
            log.error("查询流程资源失败", e);
        }
    }

    /**
     * 导出流程bpmn文件
     *
     * @param definitionId
     * @param response
     */
    public void exportProcessFile(String definitionId, HttpServletResponse response) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();
        if (processDefinition == null) {
            throw new ServerErrorException("流程定义不存在");
        }
        InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getResourceName());
        try {
            response.setContentType(MediaType.APPLICATION_XML_VALUE);
            response.setHeader("content-disposition", "attachment;filename=" + processDefinition.getResourceName());
            IOUtils.copy(resourceAsStream, response.getOutputStream());
        } catch (IOException e) {
            log.error("导出流程资源失败", e);
        }
    }

    /**
     * 查看流程运行时图
     * @param processInstanceId
     * @param response
     */
    public void viewProcessRuntimeImage(String processInstanceId, HttpServletResponse response) {
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (historicProcessInstance == null) {
            return;
        }
        ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService
                .getProcessDefinition(historicProcessInstance.getProcessDefinitionId());
        List<HistoricActivityInstance> historicActivityInstanceList = historyService
                .createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceId().desc().list();
        List<String> executedActivityIdList = new ArrayList<>();
        for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
            executedActivityIdList.add(activityInstance.getActivityId());
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        List<String> executedFlowIdList = getHighLightedFlows(bpmnModel, historicActivityInstanceList);
        ProcessDiagramGenerator processDiagramGenerator = processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator();
        InputStream imageStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", executedActivityIdList, executedFlowIdList, "黑体", "黑体", "黑体", null, 1.0);
        try {
            IOUtils.copy(imageStream, response.getOutputStream());
        } catch (IOException e) {
            log.error("查询流程资源失败", e);
        }
    }


    /**
     * 获取已经流转的线
     *
     * @param bpmnModel
     * @param historicActivityInstances
     * @return
     */
    private static List<String> getHighLightedFlows(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances) {
        // 高亮流程已发生流转的线id集合
        List<String> highLightedFlowIds = new ArrayList<>();
        // 全部活动节点
        List<FlowNode> historicActivityNodes = new ArrayList<>();
        // 已完成的历史活动节点
        List<HistoricActivityInstance> finishedActivityInstances = new ArrayList<>();
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstance.getActivityId(), true);
            historicActivityNodes.add(flowNode);
            if (historicActivityInstance.getEndTime() != null) {
                finishedActivityInstances.add(historicActivityInstance);
            }
        }
        FlowNode currentFlowNode = null;
        FlowNode targetFlowNode = null;
        for (HistoricActivityInstance currentActivityInstance : finishedActivityInstances) {
            currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currentActivityInstance.getActivityId(), true);
            List<SequenceFlow> sequenceFlows = currentFlowNode.getOutgoingFlows();
            if ("parallelGateway".equals(currentActivityInstance.getActivityType()) || "inclusiveGateway".equals(currentActivityInstance.getActivityType())) {
                for (SequenceFlow sequenceFlow : sequenceFlows) {
                    targetFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(sequenceFlow.getTargetRef(), true);
                    if (historicActivityNodes.contains(targetFlowNode)) {
                        highLightedFlowIds.add(targetFlowNode.getId());
                    }
                }
            } else {
                List<Map<String, Object>> tempMapList = new ArrayList<>();
                for (SequenceFlow sequenceFlow : sequenceFlows) {
                    for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                        if (historicActivityInstance.getActivityId().equals(sequenceFlow.getTargetRef())) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("highLightedFlowId", sequenceFlow.getId());
                            map.put("highLightedFlowStartTime", historicActivityInstance.getStartTime().getTime());
                            tempMapList.add(map);
                        }
                    }
                }
                if (!CollectionUtils.isEmpty(tempMapList)) {
                    // 遍历匹配的集合，取得开始时间最早的一个
                    long earliestStamp = 0L;
                    String highLightedFlowId = null;
                    for (Map<String, Object> map : tempMapList) {
                        long highLightedFlowStartTime = Long.valueOf(map.get("highLightedFlowStartTime").toString());
                        if (earliestStamp == 0 || earliestStamp >= highLightedFlowStartTime) {
                            highLightedFlowId = map.get("highLightedFlowId").toString();
                            earliestStamp = highLightedFlowStartTime;
                        }
                    }
                    highLightedFlowIds.add(highLightedFlowId);
                }
            }
        }
        return highLightedFlowIds;
    }

    /**
     * 流程实例列表查询
     *
     * @param ao
     * @return
     */
    public PageResult<ProcessInstanceListVo> getProcessInstanceList(ProcessInstanceListQueryAo ao) {
        Page<ProcessInstanceListVo> page = PageHelper.startPage(ao.getPageNum(), ao.getPageSize());
        StringBuffer sql = new StringBuffer("select DISTINCT c.PROC_INST_ID_ processInstanceId, c.PROC_DEF_ID_ processDefinitionId, a.NAME_ processDefinitionName,b.NAME_ currentTaskName, c.START_TIME_ createTime, \n" +
                "   c.END_TIME_ endTime, c.BUSINESS_KEY_ businessId, d.TEXT_ businessName, c.TENANT_ID_ tenantId, (case when t.END_TIME_ is null or t.END_TIME_ ='' then '未完结' ELSE '已完结' end) status \n" +
                "from ACT_HI_PROCINST c LEFT JOIN ACT_HI_ACTINST t on c.PROC_INST_ID_=t.ID_  LEFT JOIN ACT_RE_PROCDEF a on a.ID_=c.PROC_DEF_ID_ \n" +
                "LEFT JOIN ACT_RU_TASK b on c.PROC_INST_ID_=b.PROC_INST_ID_ and b.PROC_DEF_ID_=c.PROC_DEF_ID_ \n" +
                "LEFT JOIN ACT_HI_VARINST d on d.PROC_INST_ID_=c.PROC_INST_ID_ and d.NAME_='business_name' ");
        if (StringUtils.isNotBlank(ao.getTenantId())) {
            sql.append("and c.TENANT_ID_='").append(ao.getTenantId()).append("' ");
        }
        if (StringUtils.isNotBlank(ao.getProcessDefinitionName())) {
            sql.append("and a.NAME_ like concat(%,").append(ao.getProcessDefinitionName()).append(" %) ");
        }
        if (StringUtils.isNotBlank(ao.getBusinessName())) {
            sql.append(" and d.TEXT_ like CONCAT('%',").append(ao.getBusinessName()).append(",'%') ");
        }
        if (StringUtils.isNotBlank(ao.getStartTime())) {
            sql.append("and c.START_TIME_ >= DATE_FORMAT(").append(ao.getStartTime()).append(",'%Y-%m-%d') ");
        }
        if (StringUtils.isNotBlank(ao.getEndTime())) {
            sql.append("and c.START_TIME_ <= DATE_FORMAT(").append(ao.getEndTime()).append(",'%Y-%m-%d') ");
        }
        if (StringUtils.isNotBlank(ao.getCurrentTaskName())) {
            sql.append("and b.NAME_ like concat(%,").append(ao.getCurrentTaskName()).append(" %) ");
        }
        if (ao.getIsFinished()) {
            sql.append("and c.END_TIME_ is not null and c.END_TIME_ != '' ");
        }
        List<ProcessInstanceListVo> list = activitiMapper.queryRuntimeInstanceInfoList(sql.toString());
        PageResult<ProcessInstanceListVo> pageResult = new PageResult<>();
        pageResult.setRecords(list);
        pageResult.setTotalPages(page.getPages());
        pageResult.setTotalRecords(page.getTotal());
        return pageResult;
    }

    /**
     * '我'完成 流程实例列表
     *
     * @param ao
     * @return
     */
    public PageResult<ProcessInstanceListVo> personalCompleteProcessInstanceList(ProcessInstanceListQueryAo ao) {
        Page<ProcessInstanceListVo> page = PageHelper.startPage(ao.getPageNum(), ao.getPageSize());
        StringBuffer sql = new StringBuffer("select DISTINCT c.PROC_INST_ID_ processInstanceId, c.PROC_DEF_ID_ processDefinitionId, a.NAME_ processDefinitionName,b.NAME_ currentTaskName, c.START_TIME_ createTime, " +
                " c.BUSINESS_KEY_ businessId, d.TEXT_ businessName, c.END_TIME_ endTime, c.TENANT_ID_ tenantId, (case when t.END_TIME_ is null or t.END_TIME_ ='' then '未完结' ELSE '已完结' end) status " +
                " from ACT_HI_PROCINST c LEFT JOIN ACT_HI_ACTINST t on c.PROC_INST_ID_=t.ID_ " +
                " LEFT JOIN ACT_RE_PROCDEF a on a.ID_=c.PROC_DEF_ID_ " +
                " LEFT JOIN ACT_RU_TASK b on c.PROC_INST_ID_=b.PROC_INST_ID_ and b.PROC_DEF_ID_=c.PROC_DEF_ID_ " +
                " LEFT JOIN ACT_HI_VARINST d on d.PROC_INST_ID_=c.PROC_INST_ID_ and d.NAME_='business_name' where" +
                " (c.END_TIME_ is null or c.END_TIME_ = '') ");
        if(StringUtils.isNotBlank(ao.getTenantId())){
            sql.append("and c.TENANT_ID_='").append(ao.getTenantId()).append("' ");
        }
        if (StringUtils.isNotBlank(ao.getProcessDefinitionName())) {
            sql.append("and a.NAME_ like concat(%,").append(ao.getProcessDefinitionName()).append(" %) ");
        }
        if (StringUtils.isNotBlank(ao.getBusinessName())) {
            sql.append(" and d.TEXT_ like CONCAT('%',").append(ao.getBusinessName()).append(",'%') ");
        }
        if (StringUtils.isNotBlank(ao.getStartTime())) {
            sql.append("and c.START_TIME_ >= DATE_FORMAT(").append(ao.getStartTime()).append(",'%Y-%m-%d') ");
        }
        if (StringUtils.isNotBlank(ao.getEndTime())) {
            sql.append("and c.START_TIME_ <= DATE_FORMAT(").append(ao.getEndTime()).append(",'%Y-%m-%d') ");
        }
        if (StringUtils.isNotBlank(ao.getCurrentTaskName())) {
            sql.append("and b.NAME_ like concat(%,").append(ao.getCurrentTaskName()).append(" %) ");
        }
        if (ao.getIsFinished()) {
            sql.append("and c.END_TIME_ is not null and c.END_TIME_ != '' ");
        }
        if (!ao.getIsFinished()) {
            sql.append("and (c.END_TIME_ is null or c.END_TIME_ = '') ");
        }
        List<ProcessInstanceListVo> list = activitiMapper.queryRuntimeInstanceInfoList(sql.toString());
        PageResult<ProcessInstanceListVo> pageResult = new PageResult<>();
        pageResult.setRecords(list);
        pageResult.setTotalPages(page.getPages());
        pageResult.setTotalRecords(page.getTotal());
        return pageResult;
    }

    /**
     * 查询某流程实例的批注列表
     *
     * @param processInstanceId
     * @return
     */
    public List<ProcessCommentVo> getProcessComments(String processInstanceId) {
        List<ProcessCommentVo> list = new ArrayList<>();
        List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
        for (Comment comment : comments) {
            CommentEntity commentEntity = (CommentEntity) comment;
            HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().processInstanceId(commentEntity.getProcessInstanceId())
                    .taskId(commentEntity.getTaskId()).singleResult();
            List<Comment> his = taskService.getTaskComments(task.getId(), CommentEntity.TYPE_EVENT);
            ProcessCommentVo vo = new ProcessCommentVo();
            String[] msg = commentEntity.getMessage().split("_\\|_");
            vo.setAction(msg[0]);
            vo.setComment(msg[1]);
            vo.setDealTime(commentEntity.getTime());
            vo.setDealId(task.getAssignee());
            vo.setDealName(!CollectionUtils.isEmpty(his) ? his.get(0).getUserId() : "");
            vo.setTaskName(task.getName());
            list.add(vo);
        }
        return list;
    }

    /**
     * 通用任务处理,并添加批注
     *
     * @param ao
     */
    public void taskProcess(CompleteTaskAo ao, AfterTaskDealCallback afterTaskDealCallback) {
        if (StringUtils.isBlank(ao.getTaskId())) {
            throw new ServerErrorException("任务标识taskId不能为空");
        }
        if (StringUtils.isBlank(ao.getDealId())) {
            throw new ServerErrorException("任务处理人dealId不能为空");
        }
        Task task = taskService.createTaskQuery().taskId(ao.getTaskId()).taskTenantId(ao.getTenantId()).active().singleResult();
        if (task == null) {
            throw new ServerErrorException("任务不存在");
        }
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(task.getProcessDefinitionId())
                .processInstanceId(task.getProcessInstanceId()).includeProcessVariables().active().singleResult();
        if (processInstance == null) {
            throw new ServerErrorException("流程实例不存在");
        }
        if (StringUtils.isBlank(ao.getComment())) {
            throw new ServerErrorException("批注说明comment不能为空");
        }
        String msg = String.format("%s_|_%s", task.getName(), ao.getComment());
        taskService.addComment(task.getId(), task.getProcessInstanceId(), CommentEntity.TYPE_COMMENT, msg);
        ProcessDecisionEnum decisionEnum = ao.getDecision() == null ? ProcessDecisionEnum.AGREE : ProcessDecisionEnum.from(ao.getDecision());
        FlowElement nextElement = processUtil.getNextTaskElement(task, decisionEnum);
        Map<String, Object> vars = null;
        if (nextElement != null) {
            vars = this.getAssigneeList(task.getProcessInstanceId(), nextElement.getId(), processInstance.getProcessVariables());
        }
        Authentication.setAuthenticatedUserId(ao.getDealName());
        taskService.setAssignee(task.getId(), ao.getDealId());
        taskService.complete(task.getId(), vars);
        if (nextElement != null) {
            this.setNextUser(nextElement.getId(), task.getProcessInstanceId(), vars);
            //推mq 方式1
            this.pushMQ(processInstance.getProcessInstanceId(), processInstance.getBusinessKey(), processInstance.getTenantId(), nextElement.getId(), decisionEnum);
            //以接口进行直接回调 方式2
            if(afterTaskDealCallback != null){
                afterTaskDealCallback.execute(decisionEnum, processInstance.getBusinessKey());
            }
        }
    }

    private void pushMQ(String processInstanceId, String businessId, String tenantId, String taskKey, ProcessDecisionEnum decision){
        TaskCompleteMQDto dto = new TaskCompleteMQDto(processInstanceId, businessId, tenantId, taskKey, decision);
        //发送mq, 供业务方法消费后调整业务状态
        rabbitTemplate.convertAndSend(ProcessConstant.PROCESS_TASK_COMPLETE_AFTER, JSON.toJSONString(dto));
    }

    private List<String> getTargetDealerList(String taskKey, Map<String, Object> processVariables) {
        List<String> dealList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(processVariables)) {
            List<ProcessParam> arrs = JSONObject.parseObject(processVariables.get(ProcessConstant.PROCESS_PARAM).toString(), new TypeReference<List<ProcessParam>>() {
            });
            for (ProcessParam processParam : arrs) {
                if (processParam.getKey().equalsIgnoreCase(taskKey)) {
                    dealList = processParam.getAssigneeList() != null && processParam.getAssigneeList().length > 0 ? Arrays.asList(processParam.getAssigneeList()) : null;
                    break;
                }
            }
        }
        return dealList;
    }

    private Map<String, Object> getAssigneeList(String processInstanceId, String taskKey, Map<String, Object> processVariables) {
        String key = String.format("%s_assignee", taskKey).toLowerCase();
        List<String> dealList = this.getTargetDealerList(taskKey, processVariables);
        if (!CollectionUtils.isEmpty(dealList)) {
            processVariables.put(key, dealList);
            runtimeService.setVariables(processInstanceId, processVariables);
        }
        return processVariables;
    }

    private void setNextUser(String taskKey, String processInstanceId, Map<String, Object> map) {
        List<String> userIds = this.getTargetDealerList(taskKey, map);
        List<Task> currentTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().list();
        if (!CollectionUtils.isEmpty(currentTasks) && !CollectionUtils.isEmpty(userIds)) {
            if (currentTasks.size() != userIds.size()) {
                throw new ServerErrorException("环节任务数量和处理人数量不一致,请检查任务环节");
            }
            for (int a = 0; a < currentTasks.size(); a++) {
                Task task = currentTasks.get(a);
                if(StringUtils.isBlank(task.getAssignee())){
                    taskService.setAssignee(task.getId(), userIds.get(a));
                }
            }
        }
    }

    /**
     * 任务详情查看
     *
     * @param taskId
     * @return
     */
    public ProcessTaskVo getTaskDetail(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).includeProcessVariables().active().singleResult();
        if (task == null) {
            throw new ServerErrorException("任务不存在");
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).active().singleResult();
        Map<String, Object> map = task.getProcessVariables();
        ProcessTaskVo result = new ProcessTaskVo();
        result.setDealer(this.generateTaskDealStr(task.getAssignee(), task.getName()));
        result.setAction("apply".equalsIgnoreCase(task.getTaskDefinitionKey()) ? "申请" : "审批");
        result.setProcessStarterName(map.get(ProcessConstant.PROCESS_STARTER_NAME).toString());
        result.setProcessStartTime(instance.getStartTime());
        result.setTaskName(task.getName());
        result.setTaskStartTime(task.getCreateTime());
        result.setBusinessId(instance.getBusinessKey());
        result.setTenantId(task.getTenantId());
        return result;
    }

    private String generateTaskDealStr(String assignee, String name) {
        StringBuffer sb = new StringBuffer();
        if (StringUtils.isNotBlank(assignee)) {
            if (assignee.startsWith("USER")) {
                sb.append("[用户]:");
            } else if (assignee.startsWith("ROLE")) {
                sb.append("[角色]:");
            } else if (assignee.startsWith("CUSTOM")) {
                sb.append("[企业]:");
            }
        }
        return sb.append(" ").append(name).toString();
    }

    //分页
    private <T> PageResult<T> generatePageResult(List<T> list, int pageNum, int pageSize) {
        int  index = (pageNum - 1) * pageSize;
        int pos = pageNum * pageSize;
        PageResult<T> pageResult = new PageResult<>();
        List<T> subList = new ArrayList<>();
        if (list.size() >= index && list.size() <= pos) {
            subList = list.subList(index, list.size());
        } else if (list.size() > pos) {
            subList = list.subList(index, pos);
        }
        pageResult.setRecords(subList);
        float va = (float) list.size() / pageSize;
        pageResult.setTotalPages(pageSize > list.size() ? 1 : (int) Math.ceil(va));
        pageResult.setTotalRecords(list.size());
        return pageResult;
    }
}
