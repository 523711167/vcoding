package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.*;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.common.util.ObjectMapperUtils;
import com.yuyu.workflow.entity.*;
import com.yuyu.workflow.entity.base.BaseIdEntity;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.eto.workflow.WorkflowBizSubmitETO;
import com.yuyu.workflow.mapper.*;
import com.yuyu.workflow.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流发起服务实现。
 */
@Service
public class WorkflowLaunchServiceImpl implements WorkflowLaunchService {

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private final BizApplyService bizApplyService;
    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowNodeInstanceService workflowNodeInstanceService;
    private final WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService;
    private final WorkflowApprovalRecordService workflowApprovalRecordService;
    private final WorkflowNodeMapper workflowNodeMapper;
    private final WorkflowTransitionMapper workflowTransitionMapper;
    private final WorkflowNodeApproverMapper workflowNodeApproverMapper;
    private final UserMapper userMapper;
    private final UserDeptRelMapper userDeptRelMapper;
    private final UserDeptMapper userDeptMapper;
    private final ObjectMapperUtils objectMapperUtils;
    private final WorkflowRouteTreeBuilder workflowRouteTreeBuilder;
    private final BizDefinitionService bizDefinitionService;

    public WorkflowLaunchServiceImpl(BizApplyService bizApplyService,
                                     WorkflowInstanceService workflowInstanceService,
                                     WorkflowNodeInstanceService workflowNodeInstanceService,
                                     WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService,
                                     WorkflowApprovalRecordService workflowApprovalRecordService,
                                     WorkflowNodeMapper workflowNodeMapper,
                                     WorkflowTransitionMapper workflowTransitionMapper,
                                     WorkflowNodeApproverMapper workflowNodeApproverMapper,
                                     UserMapper userMapper,
                                     UserDeptRelMapper userDeptRelMapper,
                                     UserDeptMapper userDeptMapper,
                                     ObjectMapperUtils objectMapperUtils,
                                     BizDefinitionService bizDefinitionService,
                                     WorkflowRouteTreeBuilder workflowRouteTreeBuilder) {
        this.bizApplyService = bizApplyService;
        this.workflowInstanceService = workflowInstanceService;
        this.workflowNodeInstanceService = workflowNodeInstanceService;
        this.workflowNodeApproverInstanceService = workflowNodeApproverInstanceService;
        this.workflowApprovalRecordService = workflowApprovalRecordService;
        this.workflowNodeMapper = workflowNodeMapper;
        this.workflowTransitionMapper = workflowTransitionMapper;
        this.workflowNodeApproverMapper = workflowNodeApproverMapper;
        this.userMapper = userMapper;
        this.userDeptRelMapper = userDeptRelMapper;
        this.userDeptMapper = userDeptMapper;
        this.objectMapperUtils = objectMapperUtils;
        this.workflowRouteTreeBuilder = workflowRouteTreeBuilder;
        this.bizDefinitionService = bizDefinitionService;
    }

    @Override
    public void submit(WorkflowBizSubmitETO eto) {
        if (Objects.isNull(eto.getBizApplyId())) {

        }

        loadSubmitContext(eto);

        // 创建流程实例 保存业务主健
        // 从开始节点查找下个节点
        // 节点查找审核人
    }

    private SubmitContext loadSubmitContext(WorkflowBizSubmitETO eto) {

        BizDefinition bizDefinition = bizDefinitionService.getOne(
                Wrappers.<BizDefinition>lambdaQuery().eq(BizDefinition::getBizCode, eto.getBizCode()));
        Long workflowDefinitionId = bizDefinition.getWorkflowDefinitionId();

        // 整个流程定义节点连线
        List<WorkflowTransition> definitionWorkflowTransitions = workflowTransitionMapper.selectList(
                Wrappers.<WorkflowTransition>lambdaQuery()
                        .eq(WorkflowTransition::getDefinitionId, workflowDefinitionId)
                        .orderByAsc(BaseIdEntity::getId)
        );

        List<WorkflowNode> definitionWorkflowNodes = workflowNodeMapper.selectList(
                Wrappers.<WorkflowNode>lambdaQuery()
                        .eq(WorkflowNode::getDefinitionId, workflowDefinitionId)
                        .orderByAsc(BaseIdEntity::getId)
        );

        Map<Long, WorkflowNode> nodeMap = definitionWorkflowNodes.stream()
                .collect(Collectors.toMap(WorkflowNode::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Long, List<WorkflowTransition>> transitionsByFromNodeId = definitionWorkflowTransitions.stream()
                .collect(Collectors.groupingBy(
                        WorkflowTransition::getFromNodeId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return new SubmitContext(
                eto,
                nodeMap,
                transitionsByFromNodeId
        );
    }

    @Override
    public void audit(WorkflowAuditETO eto) {
        AuditContext context = loadAuditContext(eto);

        if (WorkflowAuditActionEnum.isReject(eto.getAction())) {
            handleReject(context);
            return;
        }

        handleApprove(context);
    }


    private AuditContext loadAuditContext(WorkflowAuditETO eto) {
        WorkflowInstance workflowInstance = workflowInstanceService.getById(eto.getInstanceId());
        if (workflowInstance == null) {
            throw new BizException("流程实例不存在");
        }
        if (!WorkflowInstanceStatusEnum.isRunning(workflowInstance.getStatus())) {
            throw new BizException("流程不在运行中");
        }

        WorkflowNodeInstance nodeInstance = workflowNodeInstanceService.getById(eto.getNodeInstanceId());
        if (nodeInstance == null) {
            throw new BizException("节点实例不存在");
        }
        if (!Objects.equals(nodeInstance.getInstanceId(), workflowInstance.getId())) {
            throw new BizException("节点实例不属于当前流程实例");
        }
        if (!WorkflowNodeInstanceStatusEnum.isActive(nodeInstance.getStatus())) {
            throw new BizException("当前节点不在进行中");
        }

        WorkflowNodeApproverInstance approverInstance = workflowNodeApproverInstanceService.getById(eto.getApproverInstanceId());
        if (approverInstance == null) {
            throw new BizException("审批人实例不存在");
        }
        if (!Objects.equals(approverInstance.getNodeInstanceId(), nodeInstance.getId())) {
            throw new BizException("审批人实例不属于当前节点");
        }
        if (!Objects.equals(approverInstance.getApproverId(), eto.getCurrentUserId())) {
            throw new BizException("无权审核该节点");
        }
        if (!WorkflowNodeApproverInstanceStatusEnum.isPending(approverInstance.getStatus())) {
            throw new BizException("当前审批任务不在待处理状态");
        }

        List<WorkflowNodeApproverInstance> approverInstanceList =
                workflowNodeApproverInstanceService.list(
                        Wrappers.<WorkflowNodeApproverInstance>lambdaQuery()
                                .eq(WorkflowNodeApproverInstance::getNodeInstanceId, nodeInstance.getId())
                                .orderByAsc(WorkflowNodeApproverInstance::getSortOrder, WorkflowNodeApproverInstance::getId)
                );

        WorkflowNode definitionNode = workflowNodeMapper.selectById(nodeInstance.getDefinitionNodeId());
        if (definitionNode == null) {
            throw new BizException("节点定义不存在");
        }

        // 当前节点出边
        List<WorkflowTransition> transitionList = workflowTransitionMapper.selectList(
                Wrappers.<WorkflowTransition>lambdaQuery()
                        .eq(WorkflowTransition::getFromNodeId, nodeInstance.getDefinitionNodeId())
                        .orderByAsc(WorkflowTransition::getPriority, WorkflowTransition::getId)
        );
        if (transitionList.isEmpty()) {
            throw new BizException("下一个节点不存在");
        }

        // 整个流程定义节点连线
        List<WorkflowTransition> definitionWorkflowTransitions = workflowTransitionMapper.selectList(
                Wrappers.<WorkflowTransition>lambdaQuery()
                        .eq(WorkflowTransition::getDefinitionId, definitionNode.getDefinitionId())
                        .orderByAsc(BaseIdEntity::getId)
        );

        List<WorkflowNode> definitionWorkflowNodes = workflowNodeMapper.selectList(
                Wrappers.<WorkflowNode>lambdaQuery()
                        .eq(WorkflowNode::getDefinitionId, definitionNode.getDefinitionId())
                        .orderByAsc(BaseIdEntity::getId)
        );

        Map<Long, WorkflowNode> nodeMap = definitionWorkflowNodes.stream()
                .collect(Collectors.toMap(WorkflowNode::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Long, List<WorkflowTransition>> transitionsByFromNodeId = definitionWorkflowTransitions.stream()
                .collect(Collectors.groupingBy(
                        WorkflowTransition::getFromNodeId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return new AuditContext(
                eto,
                workflowInstance,
                nodeInstance,
                approverInstance,
                approverInstanceList,
                definitionNode,
                transitionList,
                nodeMap,
                transitionsByFromNodeId
        );
    }

    private void handleApprove(AuditContext context) {
        WorkflowNodeInstance currentWorkflowNodeInstance = context.currentNodeInstance();
        // 1. 先把当前审批人实例改成已通过
        workflowNodeApproverInstanceService.updateNodeApproverForApprove(
                currentWorkflowNodeInstance.getId(),
                context.eto().getComment(),
                context.eto().getApproverInstanceId()
        );

        // 2. 查找下一个节点
        List<WorkflowTransition> workflowTransitions = context.transitionsByFromNodeId().get(currentWorkflowNodeInstance.getId());
        for (WorkflowTransition workflowTransition : workflowTransitions) {
            WorkflowNode workflowNode = context.nodeMap().get(workflowTransition.getToNodeId());
            WorkflowNodeInstance nextNodeInstance = createOrLoadParallelJoinNodeInstance(context, workflowNode);

            // 3. 写审批通过记录
            workflowApprovalRecordService.insertRecordForApprove(context.eto(), currentWorkflowNodeInstance, nextNodeInstance);

            // 4. 按审批模式判断当前节点是否已通过
            WorkflowNodeInstanceStatusEnum statusEnum = resolveNodeApproveResult(context);

            // 5. 节点还没通过，就结束本次审核
            if (WorkflowNodeInstanceStatusEnum.isPendingApproval(statusEnum.getCode())) {
                return;
            }

            AuditRuntimeContext auditRuntimeContext = new AuditRuntimeContext(statusEnum);
            // 6. 流转到下一个节点 / 结束流程
            continueAfterNodeApproved(context, auditRuntimeContext,currentWorkflowNodeInstance, nextNodeInstance);
        }
    }

    private void handleReject(AuditContext context) {
        boolean spawnParallel = context.currentNodeInstance().isSpawnParallelSplitNode();

        if (spawnParallel) {
            // 处理并行拒绝逻辑
            handleParallelReject(context);
            return;
        }

        // 处理串行拒绝逻辑
        handleSerialReject(context);
    }

    private void handleParallelReject(AuditContext context) {
        WorkflowNodeInstance workflowNodeInstance = context.currentNodeInstance();

        workflowNodeApproverInstanceService.updateNodeApproverForReject(
                workflowNodeInstance.getId(),
                context.eto().getComment(),
                context.eto().getApproverInstanceId()
        );

        WorkflowNodeInstanceStatusEnum nodeStatus = resolveNodeApproveResult(context);

        WorkflowNode workflowNode = context.nodeMap().get(workflowNodeInstance.getDefinitionNodeId());
        WorkflowNode matchJoinNode = findMatchJoinNode(workflowNode, context);
        WorkflowNodeInstance joinNodeInstance = createOrLoadParallelJoinNodeInstance(context, matchJoinNode);

        workflowApprovalRecordService.insertRecordForReject(context.eto(), workflowNodeInstance, joinNodeInstance);

        if (WorkflowNodeInstanceStatusEnum.isPendingApproval(nodeStatus.getCode())) {
            return;
        }

        processParallelJoinAfterBranchFinished(
                context,
                new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.REJECTED),
                workflowNodeInstance,
                joinNodeInstance
        );
    }

    private WorkflowNode findMatchJoinNode(WorkflowNode workflowNode, AuditContext context) {
        List<WorkflowTransition> workflowTransitions = context.transitionsByFromNodeId().get(workflowNode.getId());

        for (WorkflowTransition workflowTransition : workflowTransitions) {
            WorkflowNode nextWorkflowNode = context.nodeMap().get(workflowTransition.getToNodeId());

            if (WorkflowNodeTypeEnum.isParallelJoin(nextWorkflowNode.getNodeType())) {
                return nextWorkflowNode;
            }

            if (WorkflowNodeTypeEnum.isApproval(nextWorkflowNode.getNodeType())) {
                findMatchJoinNode(nextWorkflowNode, context);
                continue;
            }

            if (WorkflowNodeTypeEnum.isParallelSplit(nextWorkflowNode.getNodeType())) {
                findMatchJoinNode(nextWorkflowNode, context);
                continue;
            }
        }
        throw new BizException("未找到并行聚合节点");
    }

    private void processParallelJoinAfterBranchFinished(AuditContext context, AuditRuntimeContext currentNodeAuditedStatus,
                                                        WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance joinNodeInstance) {
        Long parallelBranchRootId = context.currentNodeInstance().getParallelBranchRootId();
        Long currentWorkflowNodeInstanceId = currentWorkflowNodeInstance.getId();

        // 当前分支节点通过
        if (WorkflowNodeInstanceStatusEnum.isApproved(currentNodeAuditedStatus.currentNodeAuditedStatus().getCode())) {
            // 当前分支节点通过
            workflowNodeInstanceService.update(
                    Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                            .eq(WorkflowNodeInstance::getId, currentWorkflowNodeInstanceId)
                            .eq(WorkflowNodeInstance::getParallelBranchRootId, parallelBranchRootId)
                            .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.APPROVED.getCode())
                            .set(WorkflowNodeInstance::getFinishedAt, OperationTimeContext.get())
            );
        } else if (WorkflowNodeInstanceStatusEnum.isRejected(currentNodeAuditedStatus.currentNodeAuditedStatus().getCode())) {
            workflowNodeInstanceService.update(
                    Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                            .eq(WorkflowNodeInstance::getId, currentWorkflowNodeInstanceId)
                            .eq(WorkflowNodeInstance::getParallelBranchRootId, parallelBranchRootId)
                            .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.REJECTED.getCode())
                            .set(WorkflowNodeInstance::getFinishedAt, OperationTimeContext.get())
            );

        } else {
            // PENDING_APPROVAL状态不处理
        }

        if (!allParallelBranchNodesFinished(currentWorkflowNodeInstanceId, parallelBranchRootId)) {
            // 分支未全部结束，不处理后续分支汇合后的判断
            return;
        }

        // 所有分支结束后，处理后续Join节点的审批结果
        if (hasRejectedBranch(context.workflowInstance().getId(), parallelBranchRootId)) {

            // 激活当前节点
            workflowNodeInstanceService.update(
                    Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                            .eq(BaseIdEntity::getId, joinNodeInstance.getId())
                            .eq(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.PENDING.getCode())
                            .set(WorkflowNodeInstance::getParallelBranchRootId, parallelBranchRootId)
                            .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.ACTIVE.getCode())
                            .set(WorkflowNodeInstance::getActivatedAt, OperationTimeContext.get())
            );

            WorkflowNodeInstance endNodeInstance = createOrLoadParallelJoinNodeInstance(context, WorkflowNode.toEnd());

            workflowApprovalRecordService.insertRecordForRoute(context.eto(), joinNodeInstance, endNodeInstance);

            continueAfterNodeApproved(context, new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.APPROVED),
                    joinNodeInstance, endNodeInstance);
            return;
        }

        // Join节点通过后，后续流转
        handlerJoinNode(context, joinNodeInstance);
    }

    private void handlerJoinNode(AuditContext context, WorkflowNodeInstance joinNodeInstance) {
        Long joinNodeId = joinNodeInstance.getId();
        Long definitionNodeId = joinNodeInstance.getDefinitionNodeId();

        //  激活下个节点
        workflowNodeInstanceService.update(
                Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, joinNodeId)
                        .eq(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.PENDING.getCode())
                        .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.ACTIVE.getCode())
                        .set(WorkflowNodeInstance::getFinishedAt, OperationTimeContext.get())
                        .set(WorkflowNodeInstance::getActivatedAt, OperationTimeContext.get())
        );

        // 1.根据条件判断
        List<WorkflowTransition> workflowTransitionList = context.transitionsByFromNodeId().get(definitionNodeId);

        for (WorkflowTransition workflowTransition : workflowTransitionList) {
            // 自动路由无审核人，跳过修改记录

            WorkflowNode workflowNode = context.nodeMap().get(workflowTransition.getToNodeId());
            WorkflowNodeInstance nextNodeInstance = createOrLoadParallelJoinNodeInstance(context, workflowNode);

            // 条件节点自动路由的记录
            workflowApprovalRecordService.insertRecordForRoute(
                    context.eto(),
                    joinNodeInstance,
                    nextNodeInstance
            );

            continueAfterNodeApproved(context, new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.APPROVED), joinNodeInstance, nextNodeInstance);
        }
    }

    private void handleSerialReject(AuditContext context) {
        WorkflowNodeInstance workflowNodeInstance = context.currentNodeInstance();
        WorkflowAuditETO eto = context.eto();

        // 1. 当前节点所有审核人实例
        workflowNodeApproverInstanceService.updateNodeApproverForReject(workflowNodeInstance.getId(), eto.getComment(), eto.getApproverInstanceId());
        // 2. 节点最终审核状态
        WorkflowNodeInstanceStatusEnum nodeStatus = resolveNodeApproveResult(context);
        // 3. 节点
        WorkflowNodeInstance nextNodeInstance = createOrLoadParallelJoinNodeInstance(context, WorkflowNode.toEnd());
        // 4.写入审核记录
        workflowApprovalRecordService.insertRecordForReject(eto, workflowNodeInstance, nextNodeInstance);

        if (WorkflowNodeInstanceStatusEnum.isPendingApproval(nodeStatus.getCode())) {
            return;
        }

        //  激活下个节点
        workflowNodeInstanceService.update(
                Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, nextNodeInstance.getId())
                        .eq(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.PENDING.getCode())
                        .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.ACTIVE.getCode())
                        .set(WorkflowNodeInstance::getFinishedAt, OperationTimeContext.get())
                        .set(WorkflowNodeInstance::getActivatedAt, OperationTimeContext.get())
        );

        // 修改 当前节点实例
        workflowNodeInstanceService.updateNodeForReject(workflowNodeInstance.getId(), eto.getComment());
        // 修改 当前流程实例
        workflowInstanceService.updateNodeForReject(workflowNodeInstance.getInstanceId(), WorkflowNodeInstance.toEnd());
        // 其余未处理审批人实例统一取消
        workflowNodeApproverInstanceService.cancelOtherPendingApprovers(workflowNodeInstance.getInstanceId(),
                workflowNodeInstance.getId(), eto.getApproverInstanceId());
    }


    private boolean hasRejectedBranch(Long instanceId, Long parallelBranchRootId) {
        List<WorkflowNodeInstance> nodeList = workflowNodeInstanceService.list(
                Wrappers.<WorkflowNodeInstance>lambdaQuery()
                        .eq(WorkflowNodeInstance::getInstanceId, instanceId)
                        .eq(WorkflowNodeInstance::getParallelBranchRootId, parallelBranchRootId)
        );

        return nodeList.stream()
                .anyMatch(item -> WorkflowNodeInstanceStatusEnum.REJECTED.getCode().equals(item.getStatus()));
    }


    private boolean allParallelBranchNodesFinished(Long instanceId, Long parallelBranchRootId) {
        List<WorkflowNodeInstance> nodeList = workflowNodeInstanceService.list(
                Wrappers.<WorkflowNodeInstance>lambdaQuery()
                        .eq(WorkflowNodeInstance::getInstanceId, instanceId)
                        .eq(WorkflowNodeInstance::getParallelBranchRootId, parallelBranchRootId)
        );

        return nodeList.stream().noneMatch(item -> WorkflowNodeInstanceStatusEnum.isRunningNodeInstance(item.getStatus()));
    }

    private WorkflowNodeInstance createOrLoadParallelJoinNodeInstance(AuditContext context,
                                                                      WorkflowNode nextNode) {
        Optional<WorkflowNodeInstance> exist = workflowNodeInstanceService.getOneOpt(
                Wrappers.<WorkflowNodeInstance>lambdaQuery()
                        .eq(WorkflowNodeInstance::getInstanceId, context.workflowInstance().getId())
                        .eq(WorkflowNodeInstance::getDefinitionNodeId, nextNode.getId())
                        .last("limit 1")
        );

        if (exist.isPresent()) {
            return exist.get();
        }

        WorkflowNodeInstance joinNodeInstance = new WorkflowNodeInstance();
        joinNodeInstance.setInstanceId(context.workflowInstance().getId());
        joinNodeInstance.setDefinitionNodeId(nextNode.getId());
        joinNodeInstance.setDefinitionNodeName(nextNode.getName());
        joinNodeInstance.setDefinitionNodeType(nextNode.getNodeType());
        joinNodeInstance.setParallelBranchRootId(null);
        joinNodeInstance.setStatus(WorkflowNodeInstanceStatusEnum.PENDING.getCode());

        workflowNodeInstanceService.save(joinNodeInstance);
        return joinNodeInstance;
    }


    /**
     * 解析本次操作对节点实例的审批结果
     */
    private WorkflowNodeInstanceStatusEnum resolveNodeApproveResult(AuditContext context) {
        String approveMode = context.currentNodeInstance().getApproveMode();
        String action = context.eto().getAction();

        if (WorkflowAuditActionEnum.isApprove(action)) {
            if (WorkflowApproveModeEnum.isAnd(approveMode)) {
                return handleAndApprove(context);
            }
            if (WorkflowApproveModeEnum.isOr(approveMode)) {
                return handleOrApprove(context);
            }
            if (WorkflowApproveModeEnum.isSequential(approveMode)) {
                return handleSequentialApprove(context);
            }
            return handleAndApprove(context);
        } else {
            if (WorkflowApproveModeEnum.isAnd(approveMode)) {
                return evaluateAndNodeReject(context);
            }
            if (WorkflowApproveModeEnum.isOr(approveMode)) {
                return evaluateOrNodeReject(context);
            }
            if (WorkflowApproveModeEnum.isSequential(approveMode)) {
                return evaluateSequentialNodeReject(context);
            }
            return evaluateAndNodeReject(context);
        }
    }

    private WorkflowNodeInstanceStatusEnum evaluateSequentialNodeReject(AuditContext context) {
        return WorkflowNodeInstanceStatusEnum.REJECTED;
    }

    private WorkflowNodeInstanceStatusEnum evaluateOrNodeReject(AuditContext context) {
        List<WorkflowNodeApproverInstance> pendingList =
                workflowNodeApproverInstanceService.list(
                        Wrappers.<WorkflowNodeApproverInstance>lambdaQuery()
                                .eq(WorkflowNodeApproverInstance::getNodeInstanceId, context.currentNodeInstance().getId())
                                .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
                );

        // 只要还有待处理人，节点继续保持 ACTIVE，流程继续保持 RUNNING
        if (!pendingList.isEmpty()) {
            return WorkflowNodeInstanceStatusEnum.PENDING_APPROVAL;
        }

        return WorkflowNodeInstanceStatusEnum.REJECTED;
    }

    private WorkflowNodeInstanceStatusEnum evaluateAndNodeReject(AuditContext context) {
        return WorkflowNodeInstanceStatusEnum.REJECTED;
    }

    private WorkflowNodeInstanceStatusEnum handleAndApprove(AuditContext context) {
        List<WorkflowNodeApproverInstance> pendingList = workflowNodeApproverInstanceService.list(
                Wrappers.<WorkflowNodeApproverInstance>lambdaQuery()
                        .eq(WorkflowNodeApproverInstance::getNodeInstanceId, context.currentNodeInstance().getId())
                        .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
        );

        // 当前审批人刚刚已经更新为 APPROVED
        return pendingList.isEmpty() ? WorkflowNodeInstanceStatusEnum.APPROVED : WorkflowNodeInstanceStatusEnum.PENDING_APPROVAL;
    }


    private WorkflowNodeInstanceStatusEnum handleOrApprove(AuditContext context) {
        // 当前人一通过，节点立即通过
        // 其余待处理审批人全部取消
        workflowNodeApproverInstanceService.update(
                Wrappers.<WorkflowNodeApproverInstance>lambdaUpdate()
                        .eq(WorkflowNodeApproverInstance::getNodeInstanceId, context.currentNodeInstance().getId())
                        .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
                        .ne(WorkflowNodeApproverInstance::getId, context.currentApproverInstance().getId())
                        .set(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.CANCELED.getCode())
                        .set(WorkflowNodeApproverInstance::getFinishedAt, OperationTimeContext.get())
        );
        return WorkflowNodeInstanceStatusEnum.APPROVED;
    }

    private WorkflowNodeInstanceStatusEnum handleSequentialApprove(AuditContext context) {
        WorkflowNodeApproverInstance current = context.currentApproverInstance();
        return workflowNodeApproverInstanceService.activateNextApproverInstance(current, context.approverInstanceList());

    }


    private void continueAfterNodeApproved(AuditContext context, AuditRuntimeContext auditRuntimeContext,
                                           WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance nextNodeInstance) {
        if (WorkflowNodeTypeEnum.isEnd(nextNodeInstance.getDefinitionNodeType())) {
            finishWorkflow(context, auditRuntimeContext, currentWorkflowNodeInstance, nextNodeInstance);
            return;
        }

        // 先只处理普通审批节点
        if (WorkflowNodeTypeEnum.isApproval(nextNodeInstance.getDefinitionNodeType())) {
            handlerApprovalNode(context, auditRuntimeContext, currentWorkflowNodeInstance, nextNodeInstance);
            return;
        }

        if (WorkflowNodeTypeEnum.isCondition(nextNodeInstance.getDefinitionNodeType())) {
            handlerConditionNode(context, currentWorkflowNodeInstance, nextNodeInstance);
            return;
        }

        if (WorkflowNodeTypeEnum.isParallelSplit(nextNodeInstance.getDefinitionNodeType())) {
            handlerSplitNode(context, auditRuntimeContext, currentWorkflowNodeInstance, nextNodeInstance);
            return;
        }

        if (WorkflowNodeTypeEnum.isParallelJoin(nextNodeInstance.getDefinitionNodeType())) {
            processParallelJoinAfterBranchFinished(context, auditRuntimeContext, currentWorkflowNodeInstance, nextNodeInstance);
            return;
        }

    }

    private void handlerSplitNode(AuditContext context, AuditRuntimeContext auditRuntimeContext,
                                  WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance nextNodeInstance) {
        Long definitionNodeId = nextNodeInstance.getDefinitionNodeId();
        Long currentWorkflowNodeInstanceId = currentWorkflowNodeInstance.getId();
        WorkflowNodeInstanceStatusEnum currentNodeAuditedStatus = auditRuntimeContext.currentNodeAuditedStatus();

        List<WorkflowTransition> workflowTransitionList = context.transitionsByFromNodeId().get(definitionNodeId);

        for (WorkflowTransition workflowTransition : workflowTransitionList) {
            WorkflowNode nextWorkNode = context.nodeMap().get(workflowTransition.getToNodeId());

            WorkflowNodeInstance splitNodeInstance = createOrLoadParallelJoinNodeInstance(context, nextWorkNode);

            if (WorkflowNodeInstanceStatusEnum.isApproved(currentNodeAuditedStatus.getCode())) {
                // 当前节点通过
                workflowNodeInstanceService.update(
                        Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                                .eq(WorkflowNodeInstance::getId, currentWorkflowNodeInstanceId)
                                .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.APPROVED.getCode())
                                .set(WorkflowNodeInstance::getFinishedAt, OperationTimeContext.get())
                );
            } else if (WorkflowNodeInstanceStatusEnum.isRejected(currentNodeAuditedStatus.getCode())) {
                // 后续审核方法合并后处理
            } else {
                // PENDING_APPROVAL状态不处理
            }

            // 激活下个节点
            workflowNodeInstanceService.update(
                    Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                            .eq(BaseIdEntity::getId, splitNodeInstance.getId())
                            .set(WorkflowNodeInstance::getParallelBranchRootId, currentWorkflowNodeInstanceId)
                            .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.ACTIVE.getCode())
                            .set(WorkflowNodeInstance::getActivatedAt, OperationTimeContext.get())
            );

            workflowApprovalRecordService.insertRecordForRoute(context.eto(), splitNodeInstance, nextNodeInstance);

            continueAfterNodeApproved(context, new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.APPROVED), nextNodeInstance, splitNodeInstance);
        }
    }


    private void handlerConditionNode(AuditContext context, WorkflowNodeInstance currentWorkflowNodeInstance,
                                      WorkflowNodeInstance nextNodeInstance) {
        Long definitionNodeId = currentWorkflowNodeInstance.getDefinitionNodeId();
        // 1.根据条件判断

        // 匹配节点
        WorkflowNode matchConditionNode = findMatchConditionNode(definitionNodeId, context);

        WorkflowNodeInstance matchNodeInstance = createOrLoadParallelJoinNodeInstance(context, matchConditionNode);

        // 条件节点自动路由的记录
        workflowApprovalRecordService.insertRecordForRoute(
                context.eto(),
                nextNodeInstance,
                matchNodeInstance
        );

        continueAfterNodeApproved(context, new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.APPROVED), nextNodeInstance, matchNodeInstance);
    }

    /**
     *
     */
    private WorkflowNode findMatchConditionNode(Long definitionNodeId, AuditContext context) {
        List<WorkflowTransition> workflowTransitionList = context.transitionsByFromNodeId().get(definitionNodeId);
        if (workflowTransitionList == null || workflowTransitionList.isEmpty()) {
            throw new BizException("条件节点缺少分支配置");
        }

        Map<String, Object> variables = buildConditionVariables(context.workflowInstance().getFormData());
        WorkflowTransition defaultTransition = null;

        for (WorkflowTransition workflowTransition : workflowTransitionList) {
            if (Objects.equals(workflowTransition.getIsDefault(), YesNoEnum.YES.getId())) {
                defaultTransition = workflowTransition;
                continue;
            }

            if (StringUtils.isBlank(workflowTransition.getConditionExpr())) {
                continue;
            }

            if (evaluateCondition(workflowTransition.getConditionExpr(), variables)) {
                return context.nodeMap().get(workflowTransition.getToNodeId());
            }
        }

        if (defaultTransition != null) {
            return context.nodeMap().get(defaultTransition.getToNodeId());
        }
        throw new BizException("条件节点未匹配到分支且未配置默认分支");

    }

    /**
     * 解析条件表达式上下文变量。
     */
    private Map<String, Object> buildConditionVariables(String formData) {
        if (StringUtils.isBlank(formData)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapperUtils.fromJson(formData, new TypeReference<Map<String, Object>>() {
            });
        } catch (IllegalArgumentException ex) {
            throw new BizException("流程表单数据解析失败");
        }
    }

    /**
     * 执行单条条件表达式，命中返回 true。
     */
    private boolean evaluateCondition(String conditionExpr, Map<String, Object> variables) {
        try {
            StandardEvaluationContext evaluationContext = new StandardEvaluationContext(variables);
            evaluationContext.addPropertyAccessor(new MapAccessor());
            String normalizedExpr = normalizeConditionExpr(conditionExpr);
            Boolean result = EXPRESSION_PARSER.parseExpression(normalizedExpr)
                    .getValue(evaluationContext, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception ex) {
            throw new BizException("条件表达式执行失败");
        }
    }

    /**
     * 兼容前端流程设计里常见的 JS 风格运算符。
     */
    private String normalizeConditionExpr(String conditionExpr) {
        return conditionExpr
                .replace("!==", "!=")
                .replace("===", "==")
                .replace("&&", " and ")
                .replace("||", " or ");
    }


    private void finishWorkflow(AuditContext context, AuditRuntimeContext auditRuntimeContext,
                                WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance nextNodeInstance) {
        Long nodeInstanceId = currentWorkflowNodeInstance.getId();
        Long nextNodeInstanceId = nextNodeInstance.getId();

        finishCurrentNodeAndActiveNextNode(auditRuntimeContext, currentWorkflowNodeInstance, nextNodeInstance);

        // 流程实例结束
        workflowInstanceService.update(
                Wrappers.<WorkflowInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, nodeInstanceId)
                        .set(WorkflowInstance::getStatus, auditRuntimeContext.currentNodeAuditedStatus.getCode())
                        .set(WorkflowInstance::getCurrentNodeId, nextNodeInstanceId)
                        .set(WorkflowInstance::getCurrentNodeName, WorkflowNodeTypeEnum.END.getName())
                        .set(WorkflowInstance::getCurrentNodeType, WorkflowNodeTypeEnum.END.getCode())
                        .set(WorkflowInstance::getFinishedAt, OperationTimeContext.get())
        );

        // 业务修改
        BizApply bizApply = new BizApply();
        bizApply.setId(context.workflowInstance().getBizId());
        bizApply.setBizStatus(auditRuntimeContext.currentNodeAuditedStatus.getCode());
        bizApplyService.updateById(bizApply);
    }

    private void finishCurrentNodeAndActiveNextNode(AuditRuntimeContext auditRuntimeContext,
                                                    WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance nextNodeInstance) {
        String status = auditRuntimeContext.currentNodeAuditedStatus.getCode();
        Long nodeInstanceId = currentWorkflowNodeInstance.getInstanceId();
        Long endInstanceId = nextNodeInstance.getInstanceId();
        // 当前节点通过
        workflowNodeInstanceService.update(
                Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, nodeInstanceId)
                        .set(WorkflowNodeInstance::getStatus, status)
                        .set(WorkflowNodeInstance::getFinishedAt, OperationTimeContext.get())
        );

        // 下个节点激活
        workflowNodeInstanceService.update(
                Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, endInstanceId)
                        .eq(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.PENDING.getCode())
                        .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.ACTIVE.getCode())
                        .set(WorkflowNodeInstance::getActivatedAt, OperationTimeContext.get())
                        .set(WorkflowNodeInstance::getFinishedAt, OperationTimeContext.get())
        );
    }

    private void handlerApprovalNode(AuditContext context, AuditRuntimeContext auditRuntimeContext,
                                     WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance nextNodeInstance) {
        Long nextNodeInstanceId = nextNodeInstance.getId();

        finishCurrentNodeAndActiveNextNode(auditRuntimeContext, currentWorkflowNodeInstance, nextNodeInstance);

        List<WorkflowNodeApprover> approverConfigs = workflowNodeApproverMapper.selectList(
                new LambdaQueryWrapper<WorkflowNodeApprover>()
                        .eq(WorkflowNodeApprover::getNodeId, nextNodeInstance.getDefinitionNodeId())
                        .orderByAsc(WorkflowNodeApprover::getSortOrder, WorkflowNodeApprover::getId)
        );

        String approverType = approverConfigs.stream().map(WorkflowNodeApprover::getApproverType).distinct().findFirst().get();

        if (WorkflowApproverTypeEnum.isUser(approverType)) {
            workflowNodeApproverInstanceService.saveApproverInstancesForUser(nextNodeInstance);
        }

        if (WorkflowApproverTypeEnum.isRole(approverType)) {
            workflowNodeApproverInstanceService.saveApproverInstancesForRole(nextNodeInstance);
        }

        if (WorkflowApproverTypeEnum.isDept(approverType)) {
            workflowNodeApproverInstanceService.saveApproverInstancesFordept(nextNodeInstance);
        }

        if (WorkflowApproverTypeEnum.isInitiatorDeptLeader(approverType)) {
            // 待实现
        }

        workflowInstanceService.update(
            Wrappers.<WorkflowInstance>lambdaUpdate()
                    .eq(BaseIdEntity::getId, nextNodeInstanceId)
                    .set(WorkflowInstance::getCurrentNodeId, nextNodeInstance.getId())
                    .set(WorkflowInstance::getCurrentNodeName, nextNodeInstance.getDefinitionNodeName())
                    .set(WorkflowInstance::getCurrentNodeType, nextNodeInstance.getDefinitionNodeType())
                );
    }

    private record AuditContext(
            WorkflowAuditETO eto,
            // 流程实例
            WorkflowInstance workflowInstance,
            // 当前流程节点实例
            WorkflowNodeInstance currentNodeInstance,
            // 当前审核人实例
            WorkflowNodeApproverInstance currentApproverInstance,
            // 节点审核人实例
            List<WorkflowNodeApproverInstance> approverInstanceList,
            // 流程定义节点
            WorkflowNode definitionNode,
            // 流程定义出边
            List<WorkflowTransition> transitionList,
            Map<Long, WorkflowNode> nodeMap,
            Map<Long, List<WorkflowTransition>> transitionsByFromNodeId
    ) {
    }

    private record AuditRuntimeContext(
            WorkflowNodeInstanceStatusEnum currentNodeAuditedStatus
    ) {

    }

    private record SubmitContext(
            WorkflowBizSubmitETO eto,
            // 流程实例
            Map<Long, WorkflowNode> nodeMap,
            Map<Long, List<WorkflowTransition>> transitionsByFromNodeId
    ) {
    }

}
