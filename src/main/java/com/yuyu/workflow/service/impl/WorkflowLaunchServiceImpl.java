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
import com.yuyu.workflow.struct.WorkflowLaunchStructMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final BizDefinitionService bizDefinitionService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowParallelScopeService workflowParallelScopeService;
    private final WorkflowLaunchStructMapper workflowLaunchStructMapper;
    private final WorkflowNodeService workflowNodeService;

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
                                     WorkflowDefinitionService workflowDefinitionService,
                                     WorkflowParallelScopeService workflowParallelScopeService,
                                     WorkflowLaunchStructMapper workflowLaunchStructMapper,
                                     WorkflowNodeService workflowNodeService) {
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
        this.bizDefinitionService = bizDefinitionService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.workflowParallelScopeService = workflowParallelScopeService;
        this.workflowLaunchStructMapper = workflowLaunchStructMapper;
        this.workflowNodeService = workflowNodeService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(WorkflowBizSubmitETO eto) {
        SubmitContext context = loadSubmitContext(eto);
        validateSubmit(context);

        WorkflowInstance workflowInstance = workflowInstanceService.saveStartIntance(context.bizApply(), context.workflowDefinition(), eto.getUserContextParam());

        // 开始节点
        WorkflowNode startNode = context.definitionNodeList().stream()
                .filter(item -> WorkflowNodeTypeEnum.isStart(item.getNodeType()))
                .findFirst()
                .get();
        WorkflowNodeInstance startNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(startNode, workflowInstance.getId(), null);

        Map<Long, WorkflowNode> nodeMap = context.nodeMap();
        List<WorkflowTransition> workflowTransitionList = context.transitionsByFromNodeId().get(startNode.getId());

        // 实际开始节点
        WorkflowNode actualStartNode = workflowTransitionList.stream().map(item -> nodeMap.get(item.getToNodeId())).findFirst().get();
        WorkflowNodeInstance actualStartNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(actualStartNode, workflowInstance.getId(), null);

        WorkflowAuditETO workflowAuditETO = workflowLaunchStructMapper.toWorkflowAuditETO(eto, startNodeInstance);
        workflowApprovalRecordService.recordForRoute(workflowAuditETO, startNodeInstance, actualStartNodeInstance);

        processRouteAfterNodeApproved(
                workflowLaunchStructMapper.toAuditContext(
                        workflowAuditETO,
                        workflowInstance,
                        startNodeInstance,
                        startNode,
                        context.nodeMap(),
                        context.transitionsByFromNodeId()
                ),
                new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.APPROVED),
                startNodeInstance,
                actualStartNodeInstance
        );

        // 业务修改
        BizApply bizApply = new BizApply();
        bizApply.setId(context.bizApply().getId());
        bizApply.setBizStatus(BizApplyStatusEnum.PENDING.getCode());
        bizApplyService.updateById(bizApply);
    }


    private SubmitContext loadSubmitContext(WorkflowBizSubmitETO eto) {

        Long bizId = eto.getBizApplyId();
        BizApply bizApply = bizApplyService.getByIdOrThrow(bizId);
        BizDefinition bizDefinition = bizDefinitionService.getById(bizApply.getBizDefinitionId());
        WorkflowDefinition workflowDefinition = null;

        Long workflowDefinitionId = bizDefinition != null ? bizDefinition.getWorkflowDefinitionId() : null;
        if (Objects.nonNull(workflowDefinitionId)) {
            workflowDefinition = workflowDefinitionService.getById(workflowDefinitionId);
        }

        // 整个流程定义节点连线
        List<WorkflowTransition> definitionWorkflowTransitions = Objects.isNull(workflowDefinitionId)
                ? Collections.emptyList()
                : workflowTransitionMapper.selectList(
                Wrappers.<WorkflowTransition>lambdaQuery()
                        .eq(WorkflowTransition::getDefinitionId, workflowDefinitionId)
                        .orderByAsc(BaseIdEntity::getId)
        );

        List<WorkflowNode> definitionWorkflowNodes = Objects.isNull(workflowDefinitionId)
                ? Collections.emptyList()
                : workflowNodeMapper.selectList(
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
                bizApply,
                bizDefinition,
                workflowDefinition,
                definitionWorkflowNodes,
                definitionWorkflowTransitions,
                nodeMap,
                transitionsByFromNodeId
        );
    }

    /**
     * 提交前校验业务单、业务定义、流程定义和流程图基础完整性。
     */
    private void validateSubmit(SubmitContext context) {
        bizApplyService.submitCheck(context.bizApply(), context.eto().getCurrentUserId());

        if (Objects.isNull(context.bizDefinition())) {
            throw new BizException("业务定义不存在");
        }
        if (Objects.isNull(context.bizDefinition().getWorkflowDefinitionId())) {
            throw new BizException("业务定义未绑定流程");
        }
        if (Objects.isNull(context.workflowDefinition())) {
            throw new BizException("绑定的流程定义不存在");
        }
        if (!Objects.equals(context.workflowDefinition().getStatus(), WorkflowDefinitionStatusEnum.PUBLISHED.getId())) {
            throw new BizException("绑定的流程未发布");
        }
        if (context.definitionNodeList().isEmpty()) {
            throw new BizException("流程定义缺少节点配置");
        }

        List<WorkflowNode> startNodeList = context.definitionNodeList().stream()
                .filter(item -> WorkflowNodeTypeEnum.isStart(item.getNodeType()))
                .toList();
        if (startNodeList.isEmpty()) {
            throw new BizException("流程定义缺少开始节点");
        }
        if (startNodeList.size() > 1) {
            throw new BizException("流程定义存在多个开始节点");
        }

        WorkflowNode startNode = startNodeList.get(0);
        List<WorkflowTransition> startTransitions = context.transitionsByFromNodeId()
                .getOrDefault(startNode.getId(), Collections.emptyList());
        if (startTransitions.isEmpty()) {
            throw new BizException("开始节点缺少后续连线");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
                nodeMap,
                transitionsByFromNodeId
        );
    }

    private void handleApprove(AuditContext context) {
        WorkflowNodeInstance currentWorkflowNodeInstance = context.currentNodeInstance();
        // 修改审核人
        workflowNodeApproverInstanceService.updateNodeApproverForApprove(
                currentWorkflowNodeInstance.getId(),
                context.eto().getComment(),
                context.eto().getApproverInstanceId()
        );
        //  写记录
        workflowApprovalRecordService.recordForApprove(context.eto(), currentWorkflowNodeInstance);

        // 判断节点是否通过
        WorkflowNodeInstanceStatusEnum statusEnum = resolveNodeApproveResult(
                context.eto().getAction(), context.currentApproverInstance(),
                context.currentNodeInstance(), context.approverInstanceList());

        // 节点还没通过，就结束本次审核
        if (WorkflowNodeInstanceStatusEnum.isPendingApproval(statusEnum.getCode())) {
            return;
        }

        // 当前节点通过
        List<WorkflowTransition> workflowTransitions = context.transitionsByFromNodeId().get(currentWorkflowNodeInstance.getDefinitionNodeId());
        if (workflowTransitions.size() != 1) {
            throw new BizException("流程定义设计错误：审批节点后续节点数必须为1");
        }
        for (WorkflowTransition workflowTransition : workflowTransitions) {
            WorkflowNode workflowNode = context.nodeMap().get(workflowTransition.getToNodeId());
            //  查找下一个节点
            WorkflowNodeInstance nextNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(
                    workflowNode,
                    context.workflowInstance().getId(),
                    context.currentNodeInstance().getParallelScopeId());
            //  写审批通过记录
            workflowApprovalRecordService.recordForRoute(context.eto(), currentWorkflowNodeInstance, nextNodeInstance);
            // 流转到下一个节点 / 结束流程
            processRouteAfterNodeApproved(context, new AuditRuntimeContext(statusEnum), currentWorkflowNodeInstance, nextNodeInstance);
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

        WorkflowNodeInstanceStatusEnum nodeStatus = resolveNodeApproveResult(context.eto().getAction(), context.currentApproverInstance(), context.currentNodeInstance(), context.approverInstanceList());

        WorkflowNode workflowNode = context.nodeMap().get(workflowNodeInstance.getDefinitionNodeId());
        WorkflowNode matchJoinNode = findMatchJoinNode(workflowNode, context);
        WorkflowNodeInstance joinNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(matchJoinNode, context.workflowInstance().getId(), null);

        workflowApprovalRecordService.recordForReject(context.eto(), workflowNodeInstance, joinNodeInstance);

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
                if (Objects.equals(workflowNode.getParallelSplitNodeId(), nextWorkflowNode.getParallelSplitNodeId())) {
                    return nextWorkflowNode;
                }
                findMatchJoinNode(nextWorkflowNode, context);
                continue;
            }

            if (WorkflowNodeTypeEnum.isApproval(nextWorkflowNode.getNodeType())) {
                findMatchJoinNode(nextWorkflowNode, context);
                continue;
            }

            if (WorkflowNodeTypeEnum.isParallelSplit(nextWorkflowNode.getNodeType())) {
                findMatchJoinNode(nextWorkflowNode, context);
            }
        }
        throw new BizException("未找到并行聚合节点");
    }

    private void processParallelJoinAfterBranchFinished(AuditContext context, AuditRuntimeContext currentNodeAuditedStatus,
                                                        WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance joinNodeInstance) {
        Long parallelScopeId = currentWorkflowNodeInstance.getParallelScopeId();
        Long currentWorkflowNodeInstanceId = currentWorkflowNodeInstance.getId();

        // 当前分支节点通过
        if (WorkflowNodeInstanceStatusEnum.isApproved(currentNodeAuditedStatus.currentNodeAuditedStatus().getCode())) {
            // 通过节点+1
            workflowParallelScopeService.markParallelBranchArrived(parallelScopeId);
        } else if (WorkflowNodeInstanceStatusEnum.isRejected(currentNodeAuditedStatus.currentNodeAuditedStatus().getCode())) {
            workflowNodeInstanceService.updateNodeInstanceForReject(currentWorkflowNodeInstanceId);
        } else {
            // PENDING_APPROVAL状态不处理
        }

        workflowApprovalRecordService.recordForJoinArrive(context.eto(), currentWorkflowNodeInstance, joinNodeInstance);

        if (!allParallelBranchNodesFinished(parallelScopeId)) {
            // 分支未全部结束，不处理后续分支汇合后的判断
            return;
        }

        // 所有分支结束后，处理后续Join节点的审批结果
        if (hasRejectedBranch(context.workflowInstance().getId(), parallelScopeId)) {

            WorkflowTransition workflowTransition = context.transitionsByFromNodeId().get(joinNodeInstance.getDefinitionNodeId()).get(0);
            WorkflowNode nextWorkflowNode = context.nodeMap().get(workflowTransition.getToNodeId());

            WorkflowNodeInstance nextNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(nextWorkflowNode, context.workflowInstance().getId(), null );

            workflowApprovalRecordService.recordForRoute(context.eto(), joinNodeInstance, nextNodeInstance);

            processRouteAfterNodeApproved(context, new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.REJECTED),
                    joinNodeInstance, nextNodeInstance);
            return;
        }


        // Join节点通过后，后续流转
        handlerJoinNode(context, joinNodeInstance);
    }

    private void handlerJoinNode(AuditContext context, WorkflowNodeInstance joinNodeInstance) {
        Long definitionNodeId = joinNodeInstance.getDefinitionNodeId();

        // 1.根据条件判断
        List<WorkflowTransition> workflowTransitionList = context.transitionsByFromNodeId().get(definitionNodeId);

        for (WorkflowTransition workflowTransition : workflowTransitionList) {
            // 自动路由无审核人，跳过修改记录

            WorkflowNode workflowNode = context.nodeMap().get(workflowTransition.getToNodeId());

            Long parentScopeId = workflowParallelScopeService.getParentScopeId(joinNodeInstance.getParallelScopeId());
            WorkflowNodeInstance nextNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(workflowNode, context.workflowInstance().getId(), parentScopeId);
            // 条件节点自动路由的记录
            workflowApprovalRecordService.recordForJoinPass(context.eto(), joinNodeInstance, nextNodeInstance);

            processRouteAfterNodeApproved(context, new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.APPROVED), joinNodeInstance, nextNodeInstance);
        }
    }

    private void handleSerialReject(AuditContext context) {
        WorkflowNodeInstance workflowNodeInstance = context.currentNodeInstance();
        WorkflowAuditETO eto = context.eto();

        // 1. 当前节点所有审核人实例
        workflowNodeApproverInstanceService.updateNodeApproverForReject(workflowNodeInstance.getId(), eto.getComment(), eto.getApproverInstanceId());
        // 2. 节点最终审核状态
        WorkflowNodeInstanceStatusEnum nodeStatus = resolveNodeApproveResult(context.eto().getAction(), context.currentApproverInstance(), context.currentNodeInstance(), context.approverInstanceList());
        // 3. 节点
        WorkflowNodeInstance nextNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(WorkflowNode.toEnd(),  context.workflowInstance().getId(), null );
        // 4.写入审核记录
        workflowApprovalRecordService.recordForReject(eto, workflowNodeInstance, nextNodeInstance);

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
        workflowNodeInstanceService.updateNodeInstanceForReject(workflowNodeInstance.getId());
        // 修改 当前流程实例
        workflowInstanceService.updateWorkflowInstanceForReject(workflowNodeInstance.getInstanceId(), new WorkflowNodeInstance());
        // 其余未处理审批人实例统一取消
        workflowNodeApproverInstanceService.cancelOtherPendingApprovers(workflowNodeInstance.getInstanceId(),
                workflowNodeInstance.getId(), eto.getApproverInstanceId());
    }


    private boolean hasRejectedBranch(Long instanceId, Long parallelScopeId) {
        List<WorkflowNodeInstance> nodeList = workflowNodeInstanceService.list(
                Wrappers.<WorkflowNodeInstance>lambdaQuery()
                        .eq(WorkflowNodeInstance::getInstanceId, instanceId)
                        .eq(WorkflowNodeInstance::getParallelScopeId, parallelScopeId)
        );

        return nodeList.stream()
                .anyMatch(item -> WorkflowNodeInstanceStatusEnum.REJECTED.getCode().equals(item.getStatus()));
    }


    private boolean allParallelBranchNodesFinished(Long parallelScopeId) {
        WorkflowParallelScope workflowParallelScope = workflowParallelScopeService.getById(parallelScopeId);
        return Objects.equals(workflowParallelScope.getArrivedBranchCount(), workflowParallelScope.getExpectedBranchCount());
    }

    /**
     * 解析本次操作对节点实例的审批结果
     */
    private WorkflowNodeInstanceStatusEnum resolveNodeApproveResult(String action, WorkflowNodeApproverInstance workflowNodeApproverInstance,
                                                                    WorkflowNodeInstance workflowNodeInstance, List<WorkflowNodeApproverInstance> approverInstanceList) {
        String approveMode = workflowNodeInstance.getApproveMode();

        if (WorkflowAuditActionEnum.isApprove(action)) {
            if (WorkflowApproveModeEnum.isAnd(approveMode)) {
                return handleAndApprove(workflowNodeInstance);
            }
            if (WorkflowApproveModeEnum.isOr(approveMode)) {
                return handleOrApprove(workflowNodeInstance, workflowNodeApproverInstance.getId());
            }
            if (WorkflowApproveModeEnum.isSequential(approveMode)) {
                return handleSequentialApprove(workflowNodeApproverInstance, approverInstanceList);
            }
            return handleAndApprove(workflowNodeInstance);
        } else {
            if (WorkflowApproveModeEnum.isAnd(approveMode)) {
                return evaluateAndNodeReject();
            }
            if (WorkflowApproveModeEnum.isOr(approveMode)) {
                return evaluateOrNodeReject(workflowNodeInstance);
            }
            if (WorkflowApproveModeEnum.isSequential(approveMode)) {
                return evaluateSequentialNodeReject();
            }
            return evaluateAndNodeReject();
        }
    }

    private WorkflowNodeInstanceStatusEnum evaluateSequentialNodeReject() {
        return WorkflowNodeInstanceStatusEnum.REJECTED;
    }

    private WorkflowNodeInstanceStatusEnum evaluateOrNodeReject(WorkflowNodeInstance workflowNodeInstance) {
        List<WorkflowNodeApproverInstance> pendingList =
                workflowNodeApproverInstanceService.list(
                        Wrappers.<WorkflowNodeApproverInstance>lambdaQuery()
                                .eq(WorkflowNodeApproverInstance::getNodeInstanceId, workflowNodeInstance.getId())
                                .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
                );

        // 只要还有待处理人，节点继续保持 ACTIVE，流程继续保持 RUNNING
        if (!pendingList.isEmpty()) {
            return WorkflowNodeInstanceStatusEnum.PENDING_APPROVAL;
        }

        return WorkflowNodeInstanceStatusEnum.REJECTED;
    }

    private WorkflowNodeInstanceStatusEnum evaluateAndNodeReject() {
        return WorkflowNodeInstanceStatusEnum.REJECTED;
    }

    private WorkflowNodeInstanceStatusEnum handleAndApprove(WorkflowNodeInstance workflowNodeInstance) {
        List<WorkflowNodeApproverInstance> pendingList = workflowNodeApproverInstanceService.list(
                Wrappers.<WorkflowNodeApproverInstance>lambdaQuery()
                        .eq(WorkflowNodeApproverInstance::getNodeInstanceId, workflowNodeInstance.getId())
                        .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
        );

        // 当前审批人刚刚已经更新为 APPROVED
        return pendingList.isEmpty() ? WorkflowNodeInstanceStatusEnum.APPROVED : WorkflowNodeInstanceStatusEnum.PENDING_APPROVAL;
    }


    private WorkflowNodeInstanceStatusEnum handleOrApprove(WorkflowNodeInstance workflowNodeInstance, Long approvalInstanceId) {
        // 当前人一通过，节点立即通过
        // 其余待处理审批人全部取消
        workflowNodeApproverInstanceService.update(
                Wrappers.<WorkflowNodeApproverInstance>lambdaUpdate()
                        .eq(WorkflowNodeApproverInstance::getNodeInstanceId, workflowNodeInstance.getId())
                        .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
                        .ne(WorkflowNodeApproverInstance::getId, approvalInstanceId)
                        .set(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.CANCELED.getCode())
                        .set(WorkflowNodeApproverInstance::getFinishedAt, OperationTimeContext.get())
        );
        return WorkflowNodeInstanceStatusEnum.APPROVED;
    }

    private WorkflowNodeInstanceStatusEnum handleSequentialApprove(WorkflowNodeApproverInstance current, List<WorkflowNodeApproverInstance> approverInstanceList) {
        return workflowNodeApproverInstanceService.activateNextApproverInstance(current, approverInstanceList);
    }


    /**
     *
     * @param context
     * @param auditRuntimeContext
     * @param currentWorkflowNodeInstance
     * @param nextNodeInstance
     */
    private void processRouteAfterNodeApproved(AuditContext context, AuditRuntimeContext auditRuntimeContext,
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
                                  WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance splitNodeInstance) {
        Long definitionNodeId = splitNodeInstance.getDefinitionNodeId();
        Long currentWorkflowNodeInstanceId = currentWorkflowNodeInstance.getId();
        List<WorkflowTransition> workflowTransitionList = context.transitionsByFromNodeId().get(definitionNodeId);

        workflowNodeInstanceService.updateNodeInstanceForApprove(currentWorkflowNodeInstanceId);

        WorkflowNode matchJoinNode = workflowNodeService.findMatchJoinNode(splitNodeInstance.getDefinitionNodeId(), context.workflowInstance().getDefinitionId());
        WorkflowParallelScope workflowParallelScope = workflowParallelScopeService.createOnParallelSplitEnter(
                context.workflowInstance(),
                context.nodeMap().get(definitionNodeId),
                matchJoinNode,
                currentWorkflowNodeInstance.getParallelScopeId(),
                workflowTransitionList == null ? 0 : workflowTransitionList.size()
        );

        workflowNodeInstanceService.update(
                Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, splitNodeInstance.getId())
                        .set(WorkflowNodeInstance::getParallelScopeId, workflowParallelScope.getId())
        );

        for (WorkflowTransition workflowTransition : workflowTransitionList) {
            WorkflowNode nextWorkNode = context.nodeMap().get(workflowTransition.getToNodeId());

            WorkflowNodeInstance nextSplitNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(nextWorkNode, context.workflowInstance().getId(), workflowParallelScope.getId());

            workflowApprovalRecordService.recordForSplitTrigger(context.eto(), nextSplitNodeInstance, splitNodeInstance);

            processRouteAfterNodeApproved(context, new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.APPROVED), splitNodeInstance, nextSplitNodeInstance);
        }
    }


    private void handlerConditionNode(AuditContext context, WorkflowNodeInstance currentWorkflowNodeInstance,
                                      WorkflowNodeInstance nextNodeInstance) {
        Long definitionNodeId = nextNodeInstance.getDefinitionNodeId();

        workflowNodeInstanceService.updateNodeInstanceForApprove(currentWorkflowNodeInstance.getId());

        WorkflowNode matchConditionNode = findMatchConditionNode(definitionNodeId, context);

        WorkflowNodeInstance matchNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(matchConditionNode, context.workflowInstance().getId(), currentWorkflowNodeInstance.getParallelScopeId());
        // 条件节点自动路由的记录
        workflowApprovalRecordService.recordForRoute(context.eto(), nextNodeInstance, matchNodeInstance);

        processRouteAfterNodeApproved(context, new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.APPROVED), nextNodeInstance, matchNodeInstance);
    }

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
        WorkflowNodeInstanceStatusEnum auditedStatus = auditRuntimeContext.currentNodeAuditedStatus();
        Long workInstanceId = currentWorkflowNodeInstance.getInstanceId();
        Long nodeInstanceId = currentWorkflowNodeInstance.getId();

        // 当前节点通过
        if (WorkflowNodeInstanceStatusEnum.isApproved(auditRuntimeContext.currentNodeAuditedStatus().getCode())) {
            workflowNodeInstanceService.updateNodeInstanceForApprove(nodeInstanceId);
        } else {
            workflowNodeInstanceService.updateNodeInstanceForReject(nodeInstanceId);
        }
        // 关闭结束节点
        workflowNodeInstanceService.updateNodeInstanceForEnd(nextNodeInstance);
        // 流程实例结束
        if (WorkflowNodeInstanceStatusEnum.isApproved(auditRuntimeContext.currentNodeAuditedStatus().getCode())) {
            workflowInstanceService.updateWorkflowInstanceForApproval(workInstanceId, nextNodeInstance);
        } else {
            workflowInstanceService.updateWorkflowInstanceForReject(workInstanceId, nextNodeInstance);
        }
        // 业务修改
        BizApply bizApply = new BizApply();
        bizApply.setId(context.workflowInstance().getBizId());
        bizApply.setBizStatus(BizApplyStatusEnum.toBizApplyStatusEnum(auditedStatus.getCode()).getCode());
        bizApplyService.updateById(bizApply);
    }

    private void handlerApprovalNode(AuditContext context, AuditRuntimeContext auditRuntimeContext,
                                     WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance nextNodeInstance) {
        Long nodeInstanceId = currentWorkflowNodeInstance.getId();
        String status = auditRuntimeContext.currentNodeAuditedStatus().getCode();

        if (Objects.nonNull(currentWorkflowNodeInstance.getParallelScopeId())) {
            workflowNodeInstanceService.updateScopeId(currentWorkflowNodeInstance, nextNodeInstance);
        }

        // 当前节点通过
        if (WorkflowNodeInstanceStatusEnum.isApproved(status)) {
            workflowNodeInstanceService.updateNodeInstanceForApprove(nodeInstanceId);
        } else {
            workflowNodeInstanceService.updateNodeInstanceForReject(nodeInstanceId);
        }

        saveApproverNode(nextNodeInstance);

        workflowInstanceService.updateWorkflowInstanceForSite(context.workflowInstance().getId(), nextNodeInstance);
    }

    private void saveApproverNode(WorkflowNodeInstance nextNodeInstance) {
        List<WorkflowNodeApprover> approverConfigs = workflowNodeApproverMapper.selectList(
                new LambdaQueryWrapper<WorkflowNodeApprover>()
                        .eq(WorkflowNodeApprover::getNodeId, nextNodeInstance.getDefinitionNodeId())
                        .orderByAsc(WorkflowNodeApprover::getSortOrder, WorkflowNodeApprover::getId)
        );
        if (approverConfigs.isEmpty()) {
            throw new BizException("审批节点缺少审批人配置");
        }

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
    }

    public record AuditContext(
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
            BizApply bizApply,
            BizDefinition bizDefinition,
            WorkflowDefinition workflowDefinition,
            List<WorkflowNode> definitionNodeList,
            List<WorkflowTransition> definitionTransitionList,
            Map<Long, WorkflowNode> nodeMap,
            Map<Long, List<WorkflowTransition>> transitionsByFromNodeId
    ) {
    }

    /**
     * 路由上下文
     */
    private record RouteContext(
            WorkflowInstance workflowInstance,
            Map<Long, WorkflowNode> nodeMap,
            Map<Long, List<WorkflowTransition>> transitionsByFromNodeId
    ) {

    }

}
