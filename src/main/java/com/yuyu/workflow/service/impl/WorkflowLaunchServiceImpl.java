package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.*;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.common.util.ObjectMapperUtils;
import com.yuyu.workflow.convert.WorkflowNodeApproverInstanceStructMapper;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.entity.UserDeptRelExpand;
import com.yuyu.workflow.entity.UserRoleRel;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.entity.WorkflowNodeApproverDeptExpand;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.entity.base.BaseIdEntity;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.mapper.*;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.service.WorkflowApprovalRecordService;
import com.yuyu.workflow.service.WorkflowInstanceService;
import com.yuyu.workflow.service.WorkflowLaunchService;
import com.yuyu.workflow.service.WorkflowNodeApproverInstanceService;
import com.yuyu.workflow.service.WorkflowNodeInstanceService;
import com.yuyu.workflow.service.WorkflowRouteTreeBuilder;
import com.yuyu.workflow.service.model.workflow.WorkflowRouteNode;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工作流发起服务实现。
 */
@Service
public class WorkflowLaunchServiceImpl implements WorkflowLaunchService {

    private final BizApplyService bizApplyService;
    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowNodeInstanceService workflowNodeInstanceService;
    private final WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService;
    private final WorkflowApprovalRecordService workflowApprovalRecordService;
    private final BizDefinitionMapper bizDefinitionMapper;
    private final BizDefinitionRoleRelMapper bizDefinitionRoleRelMapper;
    private final WorkflowDefinitionMapper workflowDefinitionMapper;
    private final WorkflowNodeMapper workflowNodeMapper;
    private final WorkflowTransitionMapper workflowTransitionMapper;
    private final WorkflowNodeApproverMapper workflowNodeApproverMapper;
    private final WorkflowNodeApproverDeptExpandMapper workflowNodeApproverDeptExpandMapper;
    private final UserMapper userMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final UserDeptRelMapper userDeptRelMapper;
    private final UserDeptRelExpandMapper userDeptRelExpandMapper;
    private final UserDeptMapper userDeptMapper;
    private final ObjectMapperUtils objectMapperUtils;
    private final WorkflowNodeApproverInstanceStructMapper workflowNodeApproverInstanceStructMapper;
    private final WorkflowNodeInstanceMapper workflowNodeInstanceMapper;
    private final WorkflowDefinitionServiceImpl workflowDefinitionService;
    private final WorkflowRouteTreeBuilder workflowRouteTreeBuilder;

    public WorkflowLaunchServiceImpl(BizApplyService bizApplyService,
                                     WorkflowInstanceService workflowInstanceService,
                                     WorkflowNodeInstanceService workflowNodeInstanceService,
                                     WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService,
                                     WorkflowApprovalRecordService workflowApprovalRecordService,
                                     BizDefinitionMapper bizDefinitionMapper,
                                     BizDefinitionRoleRelMapper bizDefinitionRoleRelMapper,
                                     WorkflowDefinitionMapper workflowDefinitionMapper,
                                     WorkflowNodeMapper workflowNodeMapper,
                                     WorkflowTransitionMapper workflowTransitionMapper,
                                     WorkflowNodeApproverMapper workflowNodeApproverMapper,
                                     WorkflowNodeApproverDeptExpandMapper workflowNodeApproverDeptExpandMapper,
                                     UserMapper userMapper,
                                     UserRoleRelMapper userRoleRelMapper,
                                     UserDeptRelMapper userDeptRelMapper,
                                     UserDeptRelExpandMapper userDeptRelExpandMapper,
                                     UserDeptMapper userDeptMapper,
                                     ObjectMapperUtils objectMapperUtils,
                                     WorkflowNodeApproverInstanceStructMapper workflowNodeApproverInstanceStructMapper,
                                     WorkflowNodeInstanceMapper workflowNodeInstanceMapper,
                                     WorkflowDefinitionServiceImpl workflowDefinitionService,
                                     WorkflowRouteTreeBuilder workflowRouteTreeBuilder) {
        this.bizApplyService = bizApplyService;
        this.workflowInstanceService = workflowInstanceService;
        this.workflowNodeInstanceService = workflowNodeInstanceService;
        this.workflowNodeApproverInstanceService = workflowNodeApproverInstanceService;
        this.workflowApprovalRecordService = workflowApprovalRecordService;
        this.bizDefinitionMapper = bizDefinitionMapper;
        this.bizDefinitionRoleRelMapper = bizDefinitionRoleRelMapper;
        this.workflowDefinitionMapper = workflowDefinitionMapper;
        this.workflowNodeMapper = workflowNodeMapper;
        this.workflowTransitionMapper = workflowTransitionMapper;
        this.workflowNodeApproverMapper = workflowNodeApproverMapper;
        this.workflowNodeApproverDeptExpandMapper = workflowNodeApproverDeptExpandMapper;
        this.userMapper = userMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.userDeptRelMapper = userDeptRelMapper;
        this.userDeptRelExpandMapper = userDeptRelExpandMapper;
        this.userDeptMapper = userDeptMapper;
        this.objectMapperUtils = objectMapperUtils;
        this.workflowNodeApproverInstanceStructMapper = workflowNodeApproverInstanceStructMapper;
        this.workflowNodeInstanceMapper = workflowNodeInstanceMapper;
        this.workflowDefinitionService = workflowDefinitionService;
        this.workflowRouteTreeBuilder = workflowRouteTreeBuilder;
    }
    @Override
    public void audit(WorkflowAuditETO eto) {
        OperationTimeContext.set(LocalDateTime.now());

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


        return new AuditContext(
                eto,
                workflowInstance,
                nodeInstance,
                approverInstance,
                approverInstanceList,
                definitionNode,
                transitionList,
                definitionWorkflowTransitions,
                definitionWorkflowNodes
        );
    }

    private void handleApprove(AuditContext context) {
        // 1. 先把当前审批人实例改成已通过
        workflowNodeApproverInstanceService.updateNodeApproverForApprove(
                context.currentNodeInstance().getId(),
                context.eto().getComment(),
                context.eto().getApproverInstanceId()
        );

        // 2. 查找下一个节点
        WorkflowNode nextNode = new WorkflowNode();
        WorkflowNodeInstance nextNodeInstance = createOrLoadParallelJoinNodeInstance(context, nextNode);

        // 3. 写审批通过记录
        workflowApprovalRecordService.insertRecordForApprove(context.eto(), context.currentNodeInstance(), nextNodeInstance);

        // 4. 按审批模式判断当前节点是否已通过
        boolean nodePassed = resolveNodeApproveResult(context);

        // 5. 节点还没通过，就结束本次审核
        if (!nodePassed) {
            return;
        }

        // 6. 流转到下一个节点 / 结束流程
        continueAfterNodeApproved(context, nextNodeInstance);
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
        String approveMode = context.currentNodeInstance().getApproveMode();

        if (WorkflowApproveModeEnum.isOr(approveMode)) {
            handleParallelOrReject(context);
            return;
        }

        handleParallelDirectReject(context);
    }

    private void handleParallelDirectReject(AuditContext context) {
        workflowNodeApproverInstanceService.updateNodeApproverForReject(
                context.currentNodeInstance().getId(),
                context.eto().getComment(),
                context.eto().getApproverInstanceId()
        );

        WorkflowRouteTreeBuilder.WorkflowGraphContext joinContext = buildWorkflowGraphContext(context);
        WorkflowRouteNode routeTree = workflowRouteTreeBuilder.buildNextRouteTree(
                context.currentNodeInstance().getDefinitionNodeId(), joinContext
        );
        WorkflowNode joinNode = routeTree.findNextHopNode(context.currentNodeInstance().getDefinitionNodeId(), joinContext);
        if (joinNode == null) {
            throw new BizException("未找到下个节点");
        }
        if (!WorkflowNodeTypeEnum.isParallelJoin(joinNode.getNodeType())) {
            throw new BizException("下个节点不是并行聚合节点");
        }

        // 首个达到创建join节点，后续复用
        WorkflowNodeInstance joinNodeInstance = createOrLoadParallelJoinNodeInstance(context, joinNode);
        workflowApprovalRecordService.insertRecordForReject(context.eto(), context.currentNodeInstance(), joinNodeInstance);

        workflowNodeInstanceService.updateNodeForReject(context.currentNodeInstance().getId(), context.eto().getComment());

        workflowNodeApproverInstanceService.cancelOtherPendingApprovers(
                context.workflowInstance().getId(),
                context.currentNodeInstance().getId(),
                context.eto().getApproverInstanceId()
        );

        processParallelJoinAfterBranchFinished(context, joinNodeInstance);
    }

    private void handleParallelOrReject(AuditContext context) {
        workflowNodeApproverInstanceService.updateNodeApproverForReject(
                context.currentNodeInstance().getId(),
                context.eto().getComment(),
                context.eto().getApproverInstanceId()
        );

        WorkflowRouteTreeBuilder.WorkflowGraphContext parallelJoinContext = buildWorkflowGraphContext(context);
        WorkflowRouteNode routeTree = workflowRouteTreeBuilder.buildNextRouteTree(
                context.currentNodeInstance().getDefinitionNodeId(),
                parallelJoinContext
        );
        WorkflowNode nextParallelJoinNode = routeTree.findNextHopNode(context.currentNodeInstance().getDefinitionNodeId(), parallelJoinContext);
        if (nextParallelJoinNode == null) {
            throw new BizException("未找到下个节点");
        }
        if (!WorkflowNodeTypeEnum.isParallelJoin(nextParallelJoinNode.getNodeType())) {
            throw new BizException("下个节点不是并行聚合节点");
        }

        WorkflowNodeInstance joinNodeInstance = createOrLoadParallelJoinNodeInstance(context, nextParallelJoinNode);

        workflowApprovalRecordService.insertRecordForReject(context.eto(), context.currentNodeInstance(), joinNodeInstance);

        List<WorkflowNodeApproverInstance> pendingList =
                workflowNodeApproverInstanceService.list(
                        Wrappers.<WorkflowNodeApproverInstance>lambdaQuery()
                                .eq(WorkflowNodeApproverInstance::getNodeInstanceId, context.currentNodeInstance().getId())
                                .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
                );

        // 只要还有待处理人，节点继续保持 ACTIVE，流程继续保持 RUNNING
        if (!pendingList.isEmpty()) {
            return;
        }

        workflowNodeInstanceService.updateNodeForReject(context.currentNodeInstance().getId(), context.eto().getComment());

        processParallelJoinAfterBranchFinished(context, joinNodeInstance);
    }

    private void processParallelJoinAfterBranchFinished(AuditContext context, WorkflowNodeInstance joinNodeInstance) {
        Long parallelBranchRootId = context.currentNodeInstance().getParallelBranchRootId();

        if (!allParallelBranchNodesFinished(context.workflowInstance().getId(), parallelBranchRootId)) {
            // 分支未全部结束，不处理后续分支汇合后的判断
            return;
        }

        // 所有分支结束后，处理后续Join节点的审批结果
        if (hasRejectedBranch(context.workflowInstance().getId(), parallelBranchRootId)) {
            workflowNodeInstanceService.updateNodeForReject(joinNodeInstance.getId(), "并行分支存在拒绝");
            workflowInstanceService.updateNodeForReject(context.workflowInstance().getId(), WorkflowNodeInstance.toEndWorkflowNodeInstance());
            workflowApprovalRecordService.insertRecordForReject(context.eto(), joinNodeInstance, WorkflowNodeInstance.toEndWorkflowNodeInstance());
            return;
        }
        workflowNodeInstanceService.updateNodeForApprove(joinNodeInstance.getId(), "并行分支全部通过");

        // Join节点通过后，后续流转
        handlerJoinNode(context, joinNodeInstance);
    }

    private void handlerJoinNode(AuditContext context, WorkflowNodeInstance joinNodeInstance) {
        // 1.根据条件判断
        WorkflowRouteTreeBuilder.WorkflowGraphContext workflowGraphContext = buildWorkflowGraphContext(context);
        List<WorkflowTransition> workflowTransitions = workflowGraphContext.transitionsByFromNodeId().get(joinNodeInstance.getDefinitionNodeId());

        // 下个节点
        WorkflowTransition matchWorkflowTransitions = new WorkflowTransition();
        WorkflowNode nextWorkflowNode = workflowGraphContext.nodeMap().get(matchWorkflowTransitions.getToNodeId());
        WorkflowNodeInstance nextNodeInstance = createOrLoadParallelJoinNodeInstance(context, nextWorkflowNode);

        workflowApprovalRecordService.insertRecordForRoute(
                context.eto(),
                joinNodeInstance,
                nextNodeInstance
        );

        continueAfterNodeApproved(context, nextNodeInstance);
    }

    private void activeApprovalNode(AuditContext context, WorkflowNode targetNode) {
        // 保存节点实例
        WorkflowNodeInstance workflowNodeInstance = new WorkflowNodeInstance();
        workflowNodeInstanceService.save(workflowNodeInstance);
        // 保存审批人记录 自动路由

        // 保存审批人实例

        // 更新流程实例当前节点

    }


    private void handleSerialReject(AuditContext context) {
        WorkflowNodeInstance workflowNodeInstance = context.currentNodeInstance();
        String approveMode = workflowNodeInstance.getApproveMode();

        if (WorkflowApproveModeEnum.isOr(approveMode)) {
            handleOrReject(context);
            return;
        }

        handleDirectReject(context);
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
        joinNodeInstance.setStatus(WorkflowNodeInstanceStatusEnum.ACTIVE.getCode());
        joinNodeInstance.setActivatedAt(OperationTimeContext.get());

        workflowNodeInstanceService.save(joinNodeInstance);
        return joinNodeInstance;
    }


    private void handleDirectReject(AuditContext context) {
        WorkflowAuditETO eto = context.eto();
        WorkflowNodeInstance workflowNodeInstance = context.currentNodeInstance();
        String comment = eto.getComment();
        Long approverInstanceId = eto.getApproverInstanceId();
        Long instanceId = eto.getInstanceId();
        Long nodeInstanceId = eto.getNodeInstanceId();

        // 修改 当前节点所有审核人实例
        workflowNodeApproverInstanceService.updateNodeApproverForReject(nodeInstanceId, comment, approverInstanceId);
        // 插入 审核记录
        workflowApprovalRecordService.insertRecordForReject(eto, workflowNodeInstance, WorkflowNodeInstance.toEndWorkflowNodeInstance());
        // 修改 当前节点实例
        workflowNodeInstanceService.updateNodeForReject(nodeInstanceId, comment);
        // 修改 当前流程实例
        workflowInstanceService.updateNodeForReject(instanceId, WorkflowNodeInstance.toEndWorkflowNodeInstance());
        // 其余未处理审批人实例统一取消
        workflowNodeApproverInstanceService.cancelOtherPendingApprovers(instanceId, nodeInstanceId, approverInstanceId);
    }

    private void handleOrReject(AuditContext context) {
        workflowNodeApproverInstanceService.updateNodeApproverForReject(
                context.currentNodeInstance().getId(),
                context.eto().getComment(),
                context.currentApproverInstance().getApproverId()
        );

        workflowApprovalRecordService.insertRecordForReject(
                context.eto(),
                context.currentNodeInstance(),
                WorkflowNodeInstance.toEndWorkflowNodeInstance()
        );

        List<WorkflowNodeApproverInstance> pendingList =
                workflowNodeApproverInstanceService.list(
                        Wrappers.<WorkflowNodeApproverInstance>lambdaQuery()
                                .eq(WorkflowNodeApproverInstance::getNodeInstanceId, context.currentNodeInstance().getId())
                                .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
                );

        // 只要还有待处理人，节点继续保持 ACTIVE，流程继续保持 RUNNING
        if (!pendingList.isEmpty()) {
            return;
        }

        workflowNodeInstanceService.updateNodeForReject(context.currentNodeInstance().getId(), context.eto().getComment());
        workflowInstanceService.updateNodeForReject(context.workflowInstance().getId(), WorkflowNodeInstance.toEndWorkflowNodeInstance());
    }


    private boolean resolveNodeApproveResult(AuditContext context) {
        String approveMode = context.currentNodeInstance().getApproveMode();

        if (WorkflowApproveModeEnum.isAnd(approveMode)) {
            return handleAndApprove(context);
        }

        if (WorkflowApproveModeEnum.isOr(approveMode)) {
            return handleOrApprove(context);
        }

        if (WorkflowApproveModeEnum.isSequential(approveMode)) {
            return handleSequentialApprove(context);
        }

        return false;
    }

    private boolean handleAndApprove(AuditContext context) {
        List<WorkflowNodeApproverInstance> pendingList = workflowNodeApproverInstanceService.list(
                Wrappers.<WorkflowNodeApproverInstance>lambdaQuery()
                        .eq(WorkflowNodeApproverInstance::getNodeInstanceId, context.currentNodeInstance().getId())
                        .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
        );

        // 当前审批人刚刚已经更新为 APPROVED
        return pendingList.isEmpty();
    }


    private boolean handleOrApprove(AuditContext context) {
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
        return true;
    }

    private boolean handleSequentialApprove(AuditContext context) {
        WorkflowNodeApproverInstance current = context.currentApproverInstance();
        return workflowNodeApproverInstanceService.activateNextApproverInstance(current, context.approverInstanceList());

    }


    private void continueAfterNodeApproved(AuditContext context, WorkflowNodeInstance nextNodeInstance) {
        if (WorkflowNodeTypeEnum.isEnd(nextNodeInstance.getDefinitionNodeType())) {
            finishWorkflow(context);
            return;
        }

        // 先只处理普通审批节点
        if (WorkflowNodeTypeEnum.isApproval(nextNodeInstance.getDefinitionNodeType())) {
            handlerApprovalNode(context, nextNodeInstance);
            return;
        }

        if (WorkflowNodeTypeEnum.isCondition(nextNodeInstance.getDefinitionNodeType())) {
            handlerConditionNode(context, nextNodeInstance);
            return;
        }

        if (WorkflowNodeTypeEnum.isParallelSplit(nextNodeInstance.getDefinitionNodeType())) {
            handlerSplitNode(context, nextNodeInstance);
            return;
        }

        if (WorkflowNodeTypeEnum.isParallelJoin(nextNodeInstance.getDefinitionNodeType())) {
            processParallelJoinAfterBranchFinished(context, nextNodeInstance);
            return;
        }

    }

    private void handlerSplitNode(AuditContext context, WorkflowNodeInstance nextNodeInstance) {
        List<WorkflowTransition> workflowTransitionList = new ArrayList<>();
        for (WorkflowTransition workflowTransition : workflowTransitionList) {

            // 找到node
            WorkflowNode workflowNode = new WorkflowNode();
            WorkflowNodeInstance splitNodeInstance = createOrLoadParallelJoinNodeInstance(context, workflowNode);
            
            workflowApprovalRecordService.insertRecordForRoute(context.eto(), nextNodeInstance, splitNodeInstance);
            continueAfterNodeApproved(context, splitNodeInstance);
        }
    }


    private void handlerConditionNode(AuditContext context, WorkflowNodeInstance nextNodeInstance) {
        // 1.根据条件判断
        WorkflowRouteTreeBuilder.WorkflowGraphContext workflowGraphContext = buildWorkflowGraphContext(context);
        List<WorkflowTransition> workflowTransitions = workflowGraphContext.transitionsByFromNodeId().get(nextNodeInstance.getDefinitionNodeId());

        // 晒选符合的节点
        WorkflowTransition matchWorkflowTransitions = new WorkflowTransition();
        WorkflowNode workflowNode = workflowGraphContext.nodeMap().get(matchWorkflowTransitions.getToNodeId());

        WorkflowNodeInstance matchNodeInstance = createOrLoadParallelJoinNodeInstance(context, workflowNode);
        workflowApprovalRecordService.insertRecordForRoute(
                context.eto(),
                nextNodeInstance,
                matchNodeInstance
        );

        continueAfterNodeApproved(context, matchNodeInstance);
    }

    private void finishWorkflow(AuditContext context) {
        workflowInstanceService.update(
                Wrappers.<WorkflowInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, context.workflowInstance().getId())
                        .set(WorkflowInstance::getStatus, WorkflowInstanceStatusEnum.APPROVED.getCode())
                        .set(WorkflowInstance::getCurrentNodeId, null)
                        .set(WorkflowInstance::getCurrentNodeName, WorkflowNodeTypeEnum.END.getName())
                        .set(WorkflowInstance::getCurrentNodeType, WorkflowNodeTypeEnum.END.getCode())
                        .set(WorkflowInstance::getFinishedAt, OperationTimeContext.get())
        );

        BizApply bizApply = new BizApply();
        bizApply.setId(context.workflowInstance().getBizId());
        bizApply.setBizStatus(WorkflowInstanceStatusEnum.APPROVED.getCode());
        bizApplyService.updateById(bizApply);
    }

    private void handlerApprovalNode(AuditContext context, WorkflowNodeInstance nextNodeInstance) {
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

        }

        workflowInstanceService.update(
            Wrappers.<WorkflowInstance>lambdaUpdate()
                    .eq(BaseIdEntity::getId, context.workflowInstance().getId())
                    .set(WorkflowInstance::getCurrentNodeId, nextNodeInstance.getId())
                    .set(WorkflowInstance::getCurrentNodeName, nextNodeInstance.getDefinitionNodeName())
                    .set(WorkflowInstance::getCurrentNodeType, nextNodeInstance.getDefinitionNodeType())
                );
    }

    /**
     * 从已运行流程实例反查申请人上下文，供后续激活新审批节点时解析审批人。
     */
    private RuntimeApplicantContext buildApplicantContextFromInstance(WorkflowInstance workflowInstance) {
        User applicant = userMapper.selectById(workflowInstance.getApplicantId());
        if (Objects.isNull(applicant) || !Objects.equals(applicant.getStatus(), CommonStatusEnum.ENABLED.getId())) {
            throw new BizException("申请人不存在或已停用");
        }
        UserDeptRel primaryDeptRel = userDeptRelMapper.selectOne(new LambdaQueryWrapper<UserDeptRel>()
                .eq(UserDeptRel::getUserId, workflowInstance.getApplicantId())
                .eq(UserDeptRel::getIsPrimary, YesNoEnum.YES.getId())
                .last("LIMIT 1"));
        UserDept primaryDept = null;
        if (Objects.nonNull(primaryDeptRel) && Objects.nonNull(primaryDeptRel.getDeptId())) {
            primaryDept = userDeptMapper.selectById(primaryDeptRel.getDeptId());
        }
        return new RuntimeApplicantContext(applicant, primaryDeptRel, primaryDept);
    }

    private WorkflowRouteTreeBuilder.WorkflowGraphContext buildWorkflowGraphContext(AuditContext context) {
        return workflowRouteTreeBuilder.buildGraphContext(
                context.definitionWorkflowNodes(),
                context.definitionWorkflowTransitions()
        );
    }

    private record RuntimeApplicantContext(
            User applicant,
            UserDeptRel primaryDeptRel,
            UserDept primaryDept
    ) {
    }

    private record ResolvedApprover(
            Long userId,
            String userName
    ) {
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
            List<WorkflowTransition> definitionWorkflowTransitions,
            List<WorkflowNode> definitionWorkflowNodes
    ) {

    }

}
