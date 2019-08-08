package com.shengyecapital.process.service.joy;

import com.shengyecapital.process.enums.ProcessDecisionEnum;
import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.*;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import java.util.Collection;
import java.util.List;

/**
 * 封装动态获取某个流程的下个处理环节的信息
 */
@Slf4j
@Component
public class ProcessUtil {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;

    /**
     * 获取下一个用户任务信息
     *
     * @param currentTask 当前任务环节
     * @return 下一个用户任务节点信息
     */
    public FlowElement getNextTaskElement(Task currentTask, ProcessDecisionEnum decision) {
        FlowElement task = null;
        BpmnModel bpmnModel = repositoryService.getBpmnModel(currentTask.getProcessDefinitionId());
        Collection<FlowElement> list = bpmnModel.getMainProcess().getFlowElements();
        for (FlowElement activityImpl : list) {
            if (currentTask.getTaskDefinitionKey().equals(activityImpl.getId())) {
                task = this.nextTaskDefinition(bpmnModel, activityImpl, activityImpl.getId(), decision.getStatus().toString(), currentTask.getProcessInstanceId());
                break;
            }
        }
        return (StartEvent.class.equals(task.getClass()) || EndEvent.class.equals(task.getClass())) ? null : task;
    }

    /**
     * 下一个任务节点信息, 如果下一个节点为用户任务则直接返回,
     * 如果下一个节点为排他网关, 获取排他网关Id信息, 根据排他网关Id信息和execution获取流程实例排他网关Id为key的变量值,
     * 根据变量值分别执行排他网关后线路中的el表达式, 并找到el表达式通过的线路后的用户任务
     *
     * @param activityImpl      流程节点信息
     * @param activityId        当前流程节点Id信息
     * @param elString          排他网关顺序流线段判断条件
     * @param processInstanceId 流程实例Id信息
     * @return
     */
    private FlowElement nextTaskDefinition(BpmnModel bpmnModel, FlowElement activityImpl, String activityId, String elString, String processInstanceId) {
        FlowNode ac = null;
        Object s = null;
        if (UserTask.class.equals(activityImpl.getClass()) && !activityId.equals(activityImpl.getId())) {
            return activityImpl;
        } else {
            List<SequenceFlow> outTransitions = ((FlowNode) activityImpl).getOutgoingFlows();
            List<SequenceFlow> outTransitionsTemp = null;
            for (SequenceFlow tr : outTransitions) {
                ac = (FlowNode) bpmnModel.getMainProcess().getFlowElement(tr.getTargetRef(), true);
                if (ExclusiveGateway.class.equals(ac.getClass())) {
                    outTransitionsTemp = ac.getOutgoingFlows();
                    if (StringUtils.isEmpty(elString)) {
                        elString = this.getGatewayCondition(ac.getId(), processInstanceId);
                    }
                    if (outTransitionsTemp.size() == 1) {
                        return this.nextTaskDefinition(bpmnModel, outTransitionsTemp.get(0).getTargetFlowElement(), activityId, elString, processInstanceId);
                    } else if (outTransitionsTemp.size() > 1) {
                        for (SequenceFlow tr1 : outTransitionsTemp) {
                            s = tr1.getConditionExpression();
                            if (this.isCondition(ac.getId(), StringUtils.trim(s.toString()), elString)) {
                                Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).onlyChildExecutions().singleResult();
                                if(execution != null) {
                                    runtimeService.setVariable(execution.getId(), ac.getId(), elString);
                                }
                                return this.nextTaskDefinition(bpmnModel, tr1.getTargetFlowElement(), activityId, elString, processInstanceId);
                            }
                        }
                    }
                } else if (UserTask.class.equals(ac.getClass())) {
                    return ac;
                }
            }
            return null;
        }
    }

    /**
     * 查询流程启动时设置排他网关判断条件信息
     *
     * @param gatewayId         排他网关Id信息, 流程启动时设置网关路线判断条件key为网关Id信息
     * @param processInstanceId 流程实例Id信息
     * @return
     */
    public String getGatewayCondition(String gatewayId, String processInstanceId) {
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).onlyChildExecutions().singleResult();
        Object object = runtimeService.getVariable(execution.getId(), gatewayId);
        return object == null ? "" : object.toString();
    }

    /**
     * 根据key和value判断el表达式是否通过信息
     *
     * @param key   el表达式key信息
     * @param el    el表达式信息
     * @param value el表达式传入值信息
     * @return
     */
    public boolean isCondition(String key, String el, String value) {
        ExpressionFactory factory = new ExpressionFactoryImpl();
        SimpleContext context = new SimpleContext();
        context.setVariable(key, factory.createValueExpression(value, String.class));
        ValueExpression e = factory.createValueExpression(context, el, boolean.class);
        return (Boolean) e.getValue(context);
    }

}
