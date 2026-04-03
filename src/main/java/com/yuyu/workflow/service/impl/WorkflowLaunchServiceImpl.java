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
import com.yuyu.workflow.eto.workflow.WorkflowCancelETO;
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
    private final WorkflowTransitionService workflowTransitionService;

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
                                     WorkflowNodeService workflowNodeService,
                                     WorkflowTransitionService workflowTransitionService
    ) {
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
        this.workflowTransitionService = workflowTransitionService;
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

        // 业务修改
        bizApplyService.updateForSubmitBiz(context.bizApply().getId(), workflowInstance.getId());

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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(WorkflowCancelETO eto) {
        CancelContext context = loadCancelContext(eto);
        validateCancel(context);

        workflowInstanceService.updateWorkflowInstanceForCancel(context.workflowInstance().getId());
        workflowNodeInstanceService.updateNodeInstanceForCancel(context.workflowInstance().getId());
        workflowNodeApproverInstanceService.cancelPendingApproversForInstance(context.workflowInstance().getId());
        workflowApprovalRecordService.recordForCancel(eto, context.currentNodeInstance());

        bizApplyService.updateForBizStatusCancel(context.bizApply().getId(), eto.getComment());
    }

    /**
     * 加载取消流程所需上下文。
     */
    private CancelContext loadCancelContext(WorkflowCancelETO eto) {
        WorkflowInstance workflowInstance = workflowInstanceService.getByIdOrThrow(eto.getInstanceId());
        BizApply bizApply = bizApplyService.getByIdOrThrow(workflowInstance.getBizId());
        WorkflowNodeInstance currentNodeInstance = findCurrentCancelNodeInstance(workflowInstance.getId());
        return new CancelContext(eto, workflowInstance, bizApply, currentNodeInstance);
    }

    /**
     * 校验当前用户是否可以取消流程。
     */
    private void validateCancel(CancelContext context) {
        if (!WorkflowInstanceStatusEnum.isRunning(context.workflowInstance().getStatus())) {
            throw new BizException("流程已结束，不能取消");
        }
        if (!Objects.equals(context.workflowInstance().getApplicantId(), context.eto().getCurrentUserId())) {
            throw new BizException("无权取消该流程");
        }
    }

    /**
     * 查找取消流程时用于记录审批轨迹的当前节点实例。
     */
    private WorkflowNodeInstance findCurrentCancelNodeInstance(Long instanceId) {
        List<WorkflowNodeInstance> activeNodeInstanceList = workflowNodeInstanceService.list(
                Wrappers.<WorkflowNodeInstance>lambdaQuery()
                        .eq(WorkflowNodeInstance::getInstanceId, instanceId)
                        .in(
                                WorkflowNodeInstance::getStatus,
                                WorkflowNodeInstanceStatusEnum.PENDING.getCode(),
                                WorkflowNodeInstanceStatusEnum.ACTIVE.getCode(),
                                WorkflowNodeInstanceStatusEnum.PENDING_APPROVAL.getCode()
                        )
                        .orderByAsc(BaseIdEntity::getId)
        );
        if (!activeNodeInstanceList.isEmpty()) {
            return activeNodeInstanceList.get(0);
        }

        WorkflowNodeInstance latestNodeInstance = workflowNodeInstanceService.getOne(
                Wrappers.<WorkflowNodeInstance>lambdaQuery()
                        .eq(WorkflowNodeInstance::getInstanceId, instanceId)
                        .orderByDesc(BaseIdEntity::getId)
                        .last("limit 1")
        );
        if (Objects.isNull(latestNodeInstance)) {
            throw new BizException("流程节点实例不存在");
        }
        return latestNodeInstance;
    }

    private SubmitContext loadSubmitContext(WorkflowBizSubmitETO eto) {

        Long bizId = eto.getBizApplyId();
        BizApply bizApply = bizApplyService.getByIdOrThrow(bizId);
        BizDefinition bizDefinition = bizDefinitionService.getById(bizApply.getBizDefinitionId());
        WorkflowDefinition workflowDefinition = null;

        String workflowDefinitionCode = bizDefinition != null ? bizDefinition.getWorkflowDefinitionCode() : null;
        if (StringUtils.isNotBlank(workflowDefinitionCode)) {
            workflowDefinition = workflowDefinitionService.getLatestPublishedByCode(workflowDefinitionCode);
        }
        Long workflowDefinitionId = workflowDefinition != null ? workflowDefinition.getId() : null;

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
        if (StringUtils.isBlank(context.bizDefinition().getWorkflowDefinitionCode())) {
            throw new BizException("业务定义未绑定流程");
        }
        if (Objects.isNull(context.workflowDefinition())) {
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
        if (!YesNoEnum.isYes(approverInstance.getIsActive())) {
            throw new BizException("当前审批人未激活");
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
            if (WorkflowNodeTypeEnum.isParallelJoin(nextNodeInstance.getDefinitionNodeType())) {
                workflowApprovalRecordService.recordForJoinArrive(context.eto(), currentWorkflowNodeInstance, nextNodeInstance);
            } else {
                workflowApprovalRecordService.recordForRoute(context.eto(), currentWorkflowNodeInstance, nextNodeInstance);
            }
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
        WorkflowInstance workflowInstance = context.workflowInstance();
        WorkflowAuditETO eto = context.eto();

        workflowNodeApproverInstanceService.updateNodeApproverForReject(
                workflowNodeInstance.getId(),
                eto.getComment(),
                eto.getApproverInstanceId()
        );
        workflowApprovalRecordService.recordForReject(eto, workflowNodeInstance);

        WorkflowNodeInstanceStatusEnum nodeStatus = resolveNodeApproveResult(eto.getAction(),
                context.currentApproverInstance(),
                context.currentNodeInstance(), context.approverInstanceList());
        if (WorkflowNodeInstanceStatusEnum.isPendingApproval(nodeStatus.getCode())) {
            return;
        }

        WorkflowNode matchJoinNode = workflowNodeService.findMatchJoinNode(workflowNodeInstance.getParallelScopeId());
        WorkflowNodeInstance joinNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(matchJoinNode, workflowInstance.getId(), workflowNodeInstance.getParallelScopeId());

        workflowApprovalRecordService.recordForJoinPass(eto, workflowNodeInstance, joinNodeInstance);

        // 其余未处理审批人实例统一取消
        workflowNodeApproverInstanceService.cancelOtherPendingApprovers(workflowNodeInstance.getInstanceId(),
                workflowNodeInstance.getId(), eto.getApproverInstanceId());

        processAfterNodeProcessed(
                context,
                new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.REJECTED),
                workflowNodeInstance,
                joinNodeInstance
        );
    }

    private void processAfterNodeProcessed(AuditContext context, AuditRuntimeContext currentNodeAuditedStatus,
                                           WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance joinNodeInstance) {
        Long parallelScopeId = currentWorkflowNodeInstance.getParallelScopeId();
        Long currentWorkflowNodeInstanceId = currentWorkflowNodeInstance.getId();
        WorkflowInstance workflowInstance = context.workflowInstance();

        // 当前分支节点通过
        if (WorkflowNodeInstanceStatusEnum.isApproved(currentNodeAuditedStatus.currentNodeAuditedStatus().getCode())) {
            workflowNodeInstanceService.updateNodeInstanceForApprove(currentWorkflowNodeInstanceId);
            // 通过节点+1
        } else if (WorkflowNodeInstanceStatusEnum.isRejected(currentNodeAuditedStatus.currentNodeAuditedStatus().getCode())) {
            workflowNodeInstanceService.updateNodeInstanceForReject(currentWorkflowNodeInstanceId);
        }

        workflowParallelScopeService.markParallelBranchArrived(parallelScopeId);

        if (!allParallelBranchNodesFinished(parallelScopeId)) {
            // 分支未全部结束，不处理后续分支汇合后的判断
            return;
        }

        // 所有分支结束后，处理后续Join节点的审批结果
        if (hasRejectedBranch(joinNodeInstance.getDefinitionNodeId(), workflowInstance.getDefinitionId(), workflowInstance.getId())) {
            WorkflowTransition workflowTransition = context.transitionsByFromNodeId().get(joinNodeInstance.getDefinitionNodeId()).get(0);
            WorkflowNode nextWorkflowNode = context.nodeMap().get(workflowTransition.getToNodeId());

            // 此处需要判断下个节点是否是Join节点，如果是Join节点，需要继续创建Join节点实例，否则直接结束
            if (WorkflowNodeTypeEnum.isParallelJoin(nextWorkflowNode.getNodeType())) {
                Long parentScopeId = workflowParallelScopeService.getParentScopeId(joinNodeInstance.getParallelScopeId());
                WorkflowNodeInstance nextJoinNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(nextWorkflowNode, workflowInstance.getId(), parentScopeId);
                workflowApprovalRecordService.recordForJoinPass(context.eto(), joinNodeInstance, nextJoinNodeInstance);
                processRouteAfterNodeApproved(context, new AuditRuntimeContext(WorkflowNodeInstanceStatusEnum.REJECTED),
                        joinNodeInstance, nextJoinNodeInstance);
                return;
            }
            if (!WorkflowNodeTypeEnum.isEnd(nextWorkflowNode.getNodeType())) {
                nextWorkflowNode = workflowNodeService.findEndNode(workflowInstance.getDefinitionId());
            }
            WorkflowNodeInstance nextNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(nextWorkflowNode, workflowInstance.getId(), null);
            workflowApprovalRecordService.recordForJoinPass(context.eto(), joinNodeInstance, nextNodeInstance);
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
        WorkflowInstance workflowInstance = context.workflowInstance();

        // 1. 当前节点所有审核人实例
        workflowNodeApproverInstanceService.updateNodeApproverForReject(workflowNodeInstance.getId(), eto.getComment(), eto.getApproverInstanceId());
        // 2. 记录审核记录
        workflowApprovalRecordService.recordForReject(eto, workflowNodeInstance);
        // 3. 节点审批结果
        WorkflowNodeInstanceStatusEnum nodeStatus = resolveNodeApproveResult(context.eto().getAction(), context.currentApproverInstance(), context.currentNodeInstance(), context.approverInstanceList());

        if (WorkflowNodeInstanceStatusEnum.isPendingApproval(nodeStatus.getCode())) {
            return;
        }
        // 其余未处理审批人实例统一取消
        workflowNodeApproverInstanceService.cancelOtherPendingApprovers(workflowNodeInstance.getInstanceId(),
                workflowNodeInstance.getId(), eto.getApproverInstanceId());

        // 4. 节点
        WorkflowNode endNode = workflowNodeService.findEndNode(workflowInstance.getDefinitionId());
        WorkflowNodeInstance endNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(endNode,  workflowInstance.getId(), null);
        // 5. 记录流转记录
        workflowApprovalRecordService.recordForRoute(eto, workflowNodeInstance, endNodeInstance);

        processRouteAfterNodeApproved(
                context,
                new AuditRuntimeContext(nodeStatus),
                workflowNodeInstance,
                endNodeInstance
        );
    }


    private boolean hasRejectedBranch(Long toNodeDefinitionId, Long workflowDefinitionId, Long workflowInstanceId) {
        List<WorkflowTransition> workflowTransitionList = workflowTransitionService.list(
                Wrappers.<WorkflowTransition>lambdaQuery()
                        .eq(WorkflowTransition::getToNodeId, toNodeDefinitionId)
                        .eq(WorkflowTransition::getDefinitionId, workflowDefinitionId)
        );
        List<Long> fromNodeIdList = workflowTransitionList.stream().map(WorkflowTransition::getFromNodeId).toList();

        List<WorkflowNodeInstance> nodeList = workflowNodeInstanceService.list(
                Wrappers.<WorkflowNodeInstance>lambdaQuery()
                        .eq(WorkflowNodeInstance::getInstanceId, workflowInstanceId)
                        .in(WorkflowNodeInstance::getDefinitionNodeId, fromNodeIdList)
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
            processAfterNodeProcessed(context, auditRuntimeContext, currentWorkflowNodeInstance, nextNodeInstance);
            return;
        }

    }

    private void handlerSplitNode(AuditContext context, AuditRuntimeContext auditRuntimeContext,
                                  WorkflowNodeInstance currentWorkflowNodeInstance, WorkflowNodeInstance splitNodeInstance) {
        Long definitionNodeId = splitNodeInstance.getDefinitionNodeId();
        Long currentWorkflowNodeInstanceId = currentWorkflowNodeInstance.getId();
        List<WorkflowTransition> workflowTransitionList = context.transitionsByFromNodeId().get(definitionNodeId);
        WorkflowInstance workflowInstance = context.workflowInstance();

        workflowNodeInstanceService.updateNodeInstanceForApprove(currentWorkflowNodeInstanceId);

        WorkflowNode matchJoinNode = workflowNodeService.findMatchJoinNode(splitNodeInstance.getDefinitionNodeId(), workflowInstance.getDefinitionId());
        WorkflowParallelScope workflowParallelScope = workflowParallelScopeService.createOnParallelSplitEnter(
                workflowInstance,
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

            WorkflowNodeInstance nextSplitNodeInstance = workflowNodeInstanceService.createOrLoadParallelJoinNodeInstance(nextWorkNode, workflowInstance.getId(), workflowParallelScope.getId());

            workflowApprovalRecordService.recordForSplitTrigger(context.eto(), splitNodeInstance, nextSplitNodeInstance);

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
        bizApply.setFinishedAt(OperationTimeContext.get());
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

    private record CancelContext(
            WorkflowCancelETO eto,
            WorkflowInstance workflowInstance,
            BizApply bizApply,
            WorkflowNodeInstance currentNodeInstance
    ) {
    }

}
