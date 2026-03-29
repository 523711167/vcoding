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
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
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
import com.yuyu.workflow.service.model.workflow.WorkflowRouteEdge;
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

    /**
     * 解析审批节点全部审批人，并生成审批人实例列表。
     */
    private List<WorkflowNodeApproverInstance> buildApproverInstances(Long workflowInstanceId,
                                                                      WorkflowNode node,
                                                                      WorkflowNodeInstance nodeInstance,
                                                                      List<WorkflowNodeApprover> approverConfigs,
                                                                      Map<Long, List<WorkflowNodeApproverDeptExpand>> deptExpandByApproverId,
                                                                      RuntimeApplicantContext applicantContext) {
        if (CollectionUtils.isEmpty(approverConfigs)) {
            throw new BizException("审批节点未配置审批人");
        }
        LinkedHashMap<Long, WorkflowNodeApproverInstance> deduplicatedApprovers = new LinkedHashMap<>();
        for (WorkflowNodeApprover approverConfig : approverConfigs) {
            resolveApproverUsers(approverConfig, deptExpandByApproverId, applicantContext).forEach(candidate -> {
                deduplicatedApprovers.computeIfAbsent(candidate.userId(), key ->
                        buildApproverInstance(workflowInstanceId, node, nodeInstance, approverConfig, candidate));
            });
        }
        if (deduplicatedApprovers.isEmpty()) {
            throw new BizException("审批节点未解析到有效审批人");
        }
        List<WorkflowNodeApproverInstance> approverInstances = new ArrayList<>(deduplicatedApprovers.values());
        boolean sequential = WorkflowApproveModeEnum.SEQUENTIAL.getCode().equals(node.getApproveMode());
        for (int index = 0; index < approverInstances.size(); index++) {
            approverInstances.get(index).setIsActive(sequential ? (index == 0 ? YesNoEnum.YES.getId() : YesNoEnum.NO.getId()) : YesNoEnum.YES.getId());
        }
        return approverInstances;
    }

    /**
     * 构建单个审批人实例快照。
     */
    private WorkflowNodeApproverInstance buildApproverInstance(Long workflowInstanceId,
                                                               WorkflowNode node,
                                                               WorkflowNodeInstance nodeInstance,
                                                               WorkflowNodeApprover approverConfig,
                                                               ResolvedApprover candidate) {
        WorkflowNodeApproverInstance approverInstance = new WorkflowNodeApproverInstance();
        approverInstance.setNodeInstanceId(nodeInstance.getId());
        approverInstance.setInstanceId(workflowInstanceId);
        approverInstance.setApproverId(candidate.userId());
        approverInstance.setApproverName(candidate.userName());
        approverInstance.setNodeName(node.getName());
        approverInstance.setNodeType(node.getNodeType());
        approverInstance.setRelationType(WorkflowNodeApproverRelationTypeEnum.ORIGINAL.getCode());
        approverInstance.setStatus(WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode());
        approverInstance.setSortOrder(approverConfig.getSortOrder());
        return approverInstance;
    }

    /**
     * 按审批人类型解析实际审批用户集合。
     */
    private List<ResolvedApprover> resolveApproverUsers(WorkflowNodeApprover approverConfig,
                                                        Map<Long, List<WorkflowNodeApproverDeptExpand>> deptExpandByApproverId,
                                                        RuntimeApplicantContext applicantContext) {
        return switch (approverConfig.getApproverType()) {
            case "USER" -> resolveUserApprovers(approverConfig.getApproverValue());
            case "ROLE" -> resolveRoleApprovers(approverConfig.getApproverValue());
            case "DEPT" -> resolveDeptApprovers(approverConfig.getId(), deptExpandByApproverId);
            case "INITIATOR_DEPT_LEADER" -> resolveInitiatorDeptLeader(applicantContext);
            default -> throw new BizException("不支持的审批人类型");
        };
    }

    /**
     * 解析用户型审批人。
     */
    private List<ResolvedApprover> resolveUserApprovers(Long approverValue) {
        Long userId = parseLongValue(approverValue, "审批用户配置不合法");
        User user = userMapper.selectById(userId);
        if (Objects.isNull(user) || !Objects.equals(user.getStatus(), CommonStatusEnum.ENABLED.getId())) {
            throw new BizException("审批用户不存在或已停用");
        }
        return List.of(new ResolvedApprover(user.getId(), resolveUserName(user)));
    }

    /**
     * 解析角色型审批人。
     */
    private List<ResolvedApprover> resolveRoleApprovers(Long approverValue) {
        Long roleId = parseLongValue(approverValue, "审批角色配置不合法");
        List<Long> userIds = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRel>()
                        .eq(UserRoleRel::getRoleId, roleId)
                        .orderByAsc(UserRoleRel::getId))
                .stream()
                .map(UserRoleRel::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return resolveEnabledUsers(userIds, "审批角色未解析到有效用户");
    }

    /**
     * 解析组织型审批人，按组织展开关系取覆盖范围内全部有效用户。
     */
    private List<ResolvedApprover> resolveDeptApprovers(Long approverId,
                                                        Map<Long, List<WorkflowNodeApproverDeptExpand>> deptExpandByApproverId) {
        List<Long> deptIds = deptExpandByApproverId.getOrDefault(approverId, Collections.emptyList()).stream()
                .map(WorkflowNodeApproverDeptExpand::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(deptIds)) {
            throw new BizException("审批组织未解析到有效组织");
        }
        List<Long> userIds = userDeptRelExpandMapper.selectList(new LambdaQueryWrapper<UserDeptRelExpand>()
                        .in(UserDeptRelExpand::getDeptId, deptIds)
                        .orderByAsc(UserDeptRelExpand::getId))
                .stream()
                .map(UserDeptRelExpand::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return resolveEnabledUsers(userIds, "审批组织未解析到有效用户");
    }

    /**
     * 解析发起人主组织主管审批人。
     */
    private List<ResolvedApprover> resolveInitiatorDeptLeader(RuntimeApplicantContext applicantContext) {
        if (Objects.isNull(applicantContext.primaryDept()) || Objects.isNull(applicantContext.primaryDept().getLeaderId())) {
            throw new BizException("发起人主组织未配置主管");
        }
        User leader = userMapper.selectById(applicantContext.primaryDept().getLeaderId());
        if (Objects.isNull(leader) || !Objects.equals(leader.getStatus(), CommonStatusEnum.ENABLED.getId())) {
            throw new BizException("发起人主组织主管不存在或已停用");
        }
        return List.of(new ResolvedApprover(leader.getId(), resolveUserName(leader)));
    }

    /**
     * 过滤并加载有效用户列表。
     */
    private List<ResolvedApprover> resolveEnabledUsers(List<Long> userIds, String emptyMessage) {
        if (CollectionUtils.isEmpty(userIds)) {
            throw new BizException(emptyMessage);
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .filter(user -> Objects.equals(user.getStatus(), CommonStatusEnum.ENABLED.getId()))
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<ResolvedApprover> result = userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(user -> new ResolvedApprover(user.getId(), resolveUserName(user)))
                .toList();
        if (CollectionUtils.isEmpty(result)) {
            throw new BizException(emptyMessage);
        }
        return result;
    }

    /**
     * 优先返回真实姓名，缺失时退回用户名。
     */
    private String resolveUserName(User user) {
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return user.getUsername();
    }

    /**
     * 将字符串配置解析为长整型主键值。
     */
    private Long parseLongValue(Long value, String message) {
        if (Objects.isNull(value) || value <= 0) {
            throw new BizException(message);
        }
        return value;
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

        // 2. 写审批通过记录
        workflowApprovalRecordService.insertRecordForApprove(
                context.eto(),
                context.currentNodeInstance()
        );

        // 3. 按审批模式判断当前节点是否已通过
        boolean nodePassed = resolveNodeApproveResult(context);

        // 4. 节点还没通过，就结束本次审核
        if (!nodePassed) {
            return;
        }

        // 6. 流转到下一个节点 / 结束流程
        continueAfterNodeApproved(context);
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
                context.currentNodeInstance().getDefinitionNodeId(),
                joinContext
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
        continueAfterParallelJoinApproved(context, joinNodeInstance);
    }

    private void continueAfterParallelJoinApproved(AuditContext context, WorkflowNodeInstance joinNodeInstance) {
        WorkflowRouteTreeBuilder.WorkflowGraphContext joinContext = buildWorkflowGraphContext(context);
        WorkflowRouteNode routeTree = workflowRouteTreeBuilder.buildNextRouteTree(
                joinNodeInstance.getDefinitionNodeId(),
                joinContext
        );
        List<WorkflowRouteEdge> outgoingRoutes = workflowRouteTreeBuilder.getDirectRoutes(routeTree);
        if (CollectionUtils.isEmpty(outgoingRoutes)) {
            throw new BizException("并行聚合节点缺少后续连线");
        }

        WorkflowRouteEdge routeEdge = outgoingRoutes.get(0);
        WorkflowNode targetNode = joinContext.nodeMap().get(routeEdge.toNodeId());
        if (targetNode == null) {
            throw new BizException("并行聚合后的目标节点不存在");
        }

        if (WorkflowNodeTypeEnum.isEnd(targetNode.getNodeType())) {
            workflowApprovalRecordService.insertRecordForRoute(
                    context.eto(),
                    joinNodeInstance,
                    WorkflowNodeInstance.toEndWorkflowNodeInstance()
            );
        }

        if (WorkflowNodeTypeEnum.isApproval(targetNode.getNodeType())) {
            activeApprovalNode(context, targetNode);
        }

        if (WorkflowNodeTypeEnum.isComposite(targetNode.getNodeType())) {

        }

        WorkflowNodeInstance outGoingWorkflowNodeInstance = new WorkflowNodeInstance();
        workflowNodeInstanceService.save(outGoingWorkflowNodeInstance);
        // 写 JOIN_PASS 系统记录
        workflowApprovalRecordService.insertRecordForRoute(
                context.eto(),
                joinNodeInstance,
                outGoingWorkflowNodeInstance
        );

        //
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
                                                                      WorkflowNode joinNode) {
        Optional<WorkflowNodeInstance> exist = workflowNodeInstanceService.getOneOpt(
                Wrappers.<WorkflowNodeInstance>lambdaQuery()
                        .eq(WorkflowNodeInstance::getInstanceId, context.workflowInstance().getId())
                        .eq(WorkflowNodeInstance::getDefinitionNodeId, joinNode.getId())
                        .last("limit 1")
        );

        if (exist.isPresent()) {
            return exist.get();
        }

        WorkflowNodeInstance joinNodeInstance = new WorkflowNodeInstance();
        joinNodeInstance.setInstanceId(context.workflowInstance().getId());
        joinNodeInstance.setDefinitionNodeId(joinNode.getId());
        joinNodeInstance.setDefinitionNodeName(joinNode.getName());
        joinNodeInstance.setDefinitionNodeType(joinNode.getNodeType());
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


    private void continueAfterNodeApproved(AuditContext context) {
        List<WorkflowTransition> transitionList = context.transitionList();

        // 审批节点当前先按只有一条后续有效出边处理
        WorkflowTransition transition = transitionList.get(0);

        WorkflowNode targetNode = workflowNodeMapper.selectById(transition.getToNodeId());
        if (targetNode == null) {
            throw new BizException("后续节点不存在");
        }

        if (WorkflowNodeTypeEnum.isEnd(targetNode.getNodeType())) {
            finishWorkflow(context);
            return;
        }

        // 先只处理普通审批节点
        if (WorkflowNodeTypeEnum.isApproval(targetNode.getNodeType())) {
            activateApprovalNode(context, targetNode);
            return;
        }

        if (WorkflowNodeTypeEnum.isCondition(targetNode.getNodeType())) {
            return;
        }

        if (WorkflowNodeTypeEnum.isParallelSplit(targetNode.getNodeType())) {
            return;
        }

        if (WorkflowNodeTypeEnum.isParallelJoin(targetNode.getNodeType())) {
            return;
        }

    }

    private void finishWorkflow(AuditContext context) {
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setId(context.workflowInstance().getId());
        workflowInstance.setStatus(WorkflowInstanceStatusEnum.APPROVED.getCode());
        workflowInstance.setCurrentNodeId(null);
        workflowInstance.setCurrentNodeName(WorkflowNodeTypeEnum.END.getName());
        workflowInstance.setCurrentNodeType(WorkflowNodeTypeEnum.END.getCode());
        workflowInstance.setFinishedAt(OperationTimeContext.get());
        workflowInstanceService.updateById(workflowInstance);

        // 更新审批记录
        workflowApprovalRecordService.update(
                Wrappers.<WorkflowApprovalRecord>lambdaUpdate()
                        .eq(WorkflowApprovalRecord::getInstanceId, context.workflowInstance().getId())
                        .eq(WorkflowApprovalRecord::getNodeInstanceId, context.currentNodeInstance().getId())
                        .set(WorkflowApprovalRecord::getToNodeName, WorkflowNodeTypeEnum.END.getName())
                        .set(WorkflowApprovalRecord::getToNodeType, WorkflowNodeTypeEnum.END.getCode())
        );

        BizApply bizApply = new BizApply();
        bizApply.setId(context.workflowInstance().getBizId());
        bizApply.setBizStatus(WorkflowInstanceStatusEnum.APPROVED.getCode());
        bizApplyService.updateById(bizApply);
    }

    private void activateApprovalNode(AuditContext context, WorkflowNode targetNode) {
        WorkflowNodeInstance nextNodeInstance = new WorkflowNodeInstance();
        nextNodeInstance.setInstanceId(context.workflowInstance().getId());
        nextNodeInstance.setDefinitionNodeId(targetNode.getId());
        nextNodeInstance.setDefinitionNodeName(targetNode.getName());
        nextNodeInstance.setDefinitionNodeType(targetNode.getNodeType());
        nextNodeInstance.setStatus(WorkflowNodeInstanceStatusEnum.ACTIVE.getCode());
        nextNodeInstance.setApproveMode(targetNode.getApproveMode());
        nextNodeInstance.setActivatedAt(OperationTimeContext.get());
        workflowNodeInstanceService.save(nextNodeInstance);

        List<WorkflowNodeApprover> approverConfigs = workflowNodeApproverMapper.selectList(
                new LambdaQueryWrapper<WorkflowNodeApprover>()
                        .eq(WorkflowNodeApprover::getNodeId, targetNode.getId())
                        .orderByAsc(WorkflowNodeApprover::getSortOrder, WorkflowNodeApprover::getId)
        );
        List<Long> approverIds = approverConfigs.stream()
                .map(WorkflowNodeApprover::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<WorkflowNodeApproverDeptExpand>> deptExpandByApproverId = CollectionUtils.isEmpty(approverIds)
                ? Collections.emptyMap()
                : workflowNodeApproverDeptExpandMapper.selectList(new LambdaQueryWrapper<WorkflowNodeApproverDeptExpand>()
                                .in(WorkflowNodeApproverDeptExpand::getApproverId, approverIds)
                                .orderByAsc(WorkflowNodeApproverDeptExpand::getApproverId,
                                        WorkflowNodeApproverDeptExpand::getDistance,
                                        WorkflowNodeApproverDeptExpand::getId))
                        .stream()
                        .collect(Collectors.groupingBy(WorkflowNodeApproverDeptExpand::getApproverId,
                                LinkedHashMap::new,
                                Collectors.toList()));
        RuntimeApplicantContext applicantContext = buildApplicantContextFromInstance(context.workflowInstance());
        List<WorkflowNodeApproverInstance> approverInstances = buildApproverInstances(
                context.workflowInstance().getId(),
                targetNode,
                nextNodeInstance,
                approverConfigs,
                deptExpandByApproverId,
                applicantContext
        );
        workflowNodeApproverInstanceService.saveBatch(approverInstances);

        WorkflowInstance updateInstance = new WorkflowInstance();
        updateInstance.setId(context.workflowInstance().getId());
        updateInstance.setCurrentNodeId(nextNodeInstance.getId());
        updateInstance.setCurrentNodeName(nextNodeInstance.getDefinitionNodeName());
        updateInstance.setCurrentNodeType(nextNodeInstance.getDefinitionNodeType());
        workflowInstanceService.updateById(updateInstance);
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
