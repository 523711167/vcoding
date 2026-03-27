package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.yuyu.workflow.common.constants.WorkflowRuntimeConstants;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.*;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.common.util.ObjectMapperUtils;
import com.yuyu.workflow.convert.WorkflowNodeApproverInstanceStructMapper;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.entity.BizDefinitionRoleRel;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.entity.UserDeptRelExpand;
import com.yuyu.workflow.entity.UserRoleRel;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.WorkflowDefinition;
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
import com.yuyu.workflow.service.model.workflow.WorkflowStartApproverResult;
import com.yuyu.workflow.service.model.workflow.WorkflowStartCommand;
import com.yuyu.workflow.service.model.workflow.WorkflowStartCurrentNodeResult;
import com.yuyu.workflow.service.model.workflow.WorkflowStartResult;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final String BIZ_STATUS_DRAFT = "DRAFT";
    private static final String BIZ_STATUS_PENDING = "PENDING";

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
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final WorkflowNodeInstanceMapper workflowNodeInstanceMapper;
    private final WorkflowDefinitionServiceImpl workflowDefinitionService;

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
                                     WorkflowNodeApproverInstanceStructMapper workflowNodeApproverInstanceStructMapper, WorkflowNodeInstanceMapper workflowNodeInstanceMapper, WorkflowDefinitionServiceImpl workflowDefinitionService) {
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
    }

    /**
     * 开启流程。
     *
     * <p>该方法是审批运行层的统一发起入口，按以下顺序完成整单启动：</p>
     * <p>1. 校验开启参数、业务申请状态与当前操作人身份。</p>
     * <p>2. 读取业务定义并校验当前用户是否具备该业务的发起权限。</p>
     * <p>3. 读取已发布流程定义，构建运行时节点、连线、审批人配置上下文。</p>
     * <p>4. 创建流程实例，并从开始节点递归激活首批可执行节点。</p>
     * <p>5. 回写业务申请的流程实例、流程名称、提交时间和业务状态。</p>
     * <p>6. 记录提交动作和系统自动路由动作。</p>
     *
     * <p>该方法涉及业务申请、流程实例、节点实例、审批人实例、审批记录多表写入，
     * 必须在同一事务内完成，任一环节失败都整体回滚。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowStartResult startWorkflow(WorkflowStartCommand command) {
        WorkflowStartPreparation preparation = prepareWorkflowStart(command);
        WorkflowInstance workflowInstance = createWorkflowInstance(preparation.bizApply(), preparation.workflowDefinition());
        WorkflowStartExecution execution = executeWorkflowStart(workflowInstance, preparation);
        finalizeWorkflowStart(workflowInstance, preparation, execution);
        return buildWorkflowStartResult(preparation.bizApply(), workflowInstance, execution.activationResult());
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
     * 预加载发起流程所需的业务、流程定义和运行上下文，并完成前置校验。
     */
    private WorkflowStartPreparation prepareWorkflowStart(WorkflowStartCommand command) {
        WorkflowStartCommand normalizedCommand = requireStartCommand(command);
        BizApply bizApply = bizApplyService.getByIdOrThrow(normalizedCommand.bizApplyId());
        validateSubmitPermission(bizApply, normalizedCommand.currentUserId());

        BizDefinition bizDefinition = getEnabledBizDefinitionOrThrow(bizApply.getBizDefinitionId());
        validateInitiatorRole(bizDefinition.getId(), normalizedCommand.currentUserId());
        WorkflowDefinition workflowDefinition = getPublishedWorkflowDefinitionOrThrow(bizDefinition.getWorkflowDefinitionId());
        RuntimeDefinitionContext definitionContext = buildDefinitionContext(workflowDefinition.getId());
        RuntimeApplicantContext applicantContext = buildApplicantContext(bizApply);
        return new WorkflowStartPreparation(bizApply, workflowDefinition, definitionContext, applicantContext);
    }

    /**
     * 创建并落库流程实例初始记录。
     */
    private WorkflowInstance createWorkflowInstance(BizApply bizApply, WorkflowDefinition workflowDefinition) {
        WorkflowInstance workflowInstance = buildWorkflowInstance(bizApply, workflowDefinition);
        workflowInstanceService.save(workflowInstance);
        return workflowInstance;
    }

    /**
     * 执行从开始节点出发的首批节点激活，并汇总系统自动动作记录。
     */
    private WorkflowStartExecution executeWorkflowStart(WorkflowInstance workflowInstance,
                                                        WorkflowStartPreparation preparation) {
        List<WorkflowApprovalRecord> systemRecords = new ArrayList<>();
        ActivationResult activationResult = activateFromStartNode(
                preparation.definitionContext().startNodeId(),
                preparation.definitionContext(),
                workflowInstance,
                preparation.bizApply(),
                preparation.applicantContext(),
                systemRecords
        );
        return new WorkflowStartExecution(activationResult, systemRecords);
    }

    /**
     * 回写流程实例、业务申请以及审批记录，完成一次完整的发起动作。
     */
    private void finalizeWorkflowStart(WorkflowInstance workflowInstance,
                                       WorkflowStartPreparation preparation,
                                       WorkflowStartExecution execution) {
        updateWorkflowInstanceAfterStart(workflowInstance, execution.activationResult());
        updateBizApplyAfterStart(preparation.bizApply(), preparation.workflowDefinition(), workflowInstance, execution.activationResult());
        saveStartRecords(workflowInstance.getId(), execution, preparation.applicantContext());
    }

    /**
     * 根据首批激活结果回写流程实例当前节点快照与最终状态。
     */
    private void updateWorkflowInstanceAfterStart(WorkflowInstance workflowInstance, ActivationResult activationResult) {
        workflowInstance.setCurrentNodeId(activationResult.currentNodeId());
        workflowInstance.setCurrentNodeName(activationResult.currentNodeName());
        workflowInstance.setCurrentNodeType(activationResult.currentNodeType());
        if (activationResult.finished()) {
            workflowInstance.setCurrentNodeId(null);
            workflowInstance.setCurrentNodeName(null);
            workflowInstance.setCurrentNodeType(null);
            workflowInstance.setStatus(WorkflowInstanceStatusEnum.APPROVED.getCode());
            workflowInstance.setFinishedAt(LocalDateTime.now());
        }
        workflowInstanceService.updateById(workflowInstance);
    }

    /**
     * 回写业务申请的流程实例关联、流程名称、提交时间和业务状态。
     */
    private void updateBizApplyAfterStart(BizApply bizApply,
                                          WorkflowDefinition workflowDefinition,
                                          WorkflowInstance workflowInstance,
                                          ActivationResult activationResult) {
        bizApply.setWorkflowInstanceId(workflowInstance.getId());
        bizApply.setWorkflowName(workflowDefinition.getName());
        bizApply.setSubmittedAt(LocalDateTime.now());
        bizApply.setBizStatus(activationResult.finished() ? WorkflowInstanceStatusEnum.APPROVED.getCode() : BIZ_STATUS_PENDING);
        if (activationResult.finished()) {
            bizApply.setFinishedAt(LocalDateTime.now());
        }
        bizApplyService.updateById(bizApply);
    }

    /**
     * 保存提交动作与系统自动路由动作记录。
     */
    private void saveStartRecords(Long workflowInstanceId,
                                  WorkflowStartExecution execution,
                                  RuntimeApplicantContext applicantContext) {
        workflowApprovalRecordService.save(buildSubmitRecord(
                workflowInstanceId,
                execution.activationResult().currentNodeInstance(),
                applicantContext.applicant()
        ));
        workflowApprovalRecordService.saveBatch(execution.systemRecords());
    }

    /**
     * 组装发起流程返回结果。
     */
    private WorkflowStartResult buildWorkflowStartResult(BizApply bizApply,
                                                         WorkflowInstance workflowInstance,
                                                         ActivationResult activationResult) {
        return new WorkflowStartResult(
                bizApply.getId(),
                workflowInstance.getId(),
                buildCurrentNodeResult(activationResult.currentNodeInstance(), activationResult.currentApproverInstances())
        );
    }

    /**
     * 校验并规范化开启流程命令。
     */
    private WorkflowStartCommand requireStartCommand(WorkflowStartCommand command) {
        if (Objects.isNull(command)) {
            throw new BizException("开启流程参数不能为空");
        }
        if (Objects.isNull(command.bizApplyId())) {
            throw new BizException("bizApplyId不能为空");
        }
        return command;
    }

    /**
     * 校验业务申请是否允许由当前用户提交发起。
     */
    private void validateSubmitPermission(BizApply bizApply, Long currentUserId) {
        if (Objects.isNull(currentUserId)) {
            throw new BizException("当前登录用户不存在");
        }
        if (!Objects.equals(bizApply.getApplicantId(), currentUserId)) {
            throw new BizException("仅申请人本人允许提交审批");
        }
        if (!Objects.equals(BIZ_STATUS_DRAFT, bizApply.getBizStatus())) {
            throw new BizException("仅草稿状态业务申请允许提交审批");
        }
        if (Objects.nonNull(bizApply.getWorkflowInstanceId())) {
            throw new BizException("当前业务申请已绑定流程实例，不允许重复提交");
        }
    }

    /**
     * 查询启用状态的业务定义，不存在则抛出异常。
     */
    private BizDefinition getEnabledBizDefinitionOrThrow(Long bizDefinitionId) {
        if (Objects.isNull(bizDefinitionId)) {
            throw new BizException("业务申请未绑定业务定义");
        }
        BizDefinition bizDefinition = bizDefinitionMapper.selectById(bizDefinitionId);
        if (Objects.isNull(bizDefinition)) {
            throw new BizException("业务定义不存在或已停用");
        }
        if (!Objects.equals(bizDefinition.getStatus(), CommonStatusEnum.ENABLED.getId())) {
            throw new BizException("业务定义不存在或已停用");
        }
        return bizDefinition;
    }

    /**
     * 校验当前用户角色是否具备该业务的发起权限。
     */
    private void validateInitiatorRole(Long bizDefinitionId, Long currentUserId) {
        List<Long> roleIds = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRel>()
                        .eq(UserRoleRel::getUserId, currentUserId)
                        .orderByAsc(UserRoleRel::getId))
                .stream()
                .map(UserRoleRel::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(roleIds)) {
            throw new BizException("当前用户未配置发起角色");
        }
        boolean matched = bizDefinitionRoleRelMapper.selectList(new LambdaQueryWrapper<BizDefinitionRoleRel>()
                        .eq(BizDefinitionRoleRel::getBizDefinitionId, bizDefinitionId)
                        .in(BizDefinitionRoleRel::getRoleId, roleIds))
                .stream()
                .findAny()
                .isPresent();
        if (!matched) {
            throw new BizException("当前用户无权发起该业务");
        }
    }

    /**
     * 查询已发布流程定义，不满足发布态则拒绝发起。
     */
    private WorkflowDefinition getPublishedWorkflowDefinitionOrThrow(Long workflowDefinitionId) {
        WorkflowDefinition workflowDefinition = workflowDefinitionMapper.selectById(workflowDefinitionId);
        if (Objects.isNull(workflowDefinition)) {
            throw new BizException("绑定的流程定义不存在");
        }
        return workflowDefinition;
    }

    /**
     * 构建流程定义运行上下文，集中缓存节点、连线和审批人配置。
     */
    private RuntimeDefinitionContext buildDefinitionContext(Long definitionId) {
        List<WorkflowNode> nodeList = workflowNodeMapper.selectList(new LambdaQueryWrapper<WorkflowNode>()
                .eq(WorkflowNode::getDefinitionId, definitionId)
                .orderByAsc(WorkflowNode::getId));
        Long startNodeId = nodeList.stream()
                .filter(node -> WorkflowNodeTypeEnum.START.getCode().equals(node.getNodeType()))
                .map(WorkflowNode::getId)
                .findFirst()
                .orElse(null);
        Map<Long, WorkflowNode> nodeMap = nodeList.stream()
                .collect(Collectors.toMap(WorkflowNode::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<WorkflowTransition>> transitionsByFromNodeId = workflowTransitionMapper.selectList(
                        new LambdaQueryWrapper<WorkflowTransition>()
                                .eq(WorkflowTransition::getDefinitionId, definitionId)
                                .orderByAsc(WorkflowTransition::getPriority, WorkflowTransition::getId))
                .stream()
                .collect(Collectors.groupingBy(WorkflowTransition::getFromNodeId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<WorkflowNodeApprover>> approversByNodeId = workflowNodeApproverMapper.selectList(
                        new LambdaQueryWrapper<WorkflowNodeApprover>()
                                .eq(WorkflowNodeApprover::getDefinitionId, definitionId)
                                .orderByAsc(WorkflowNodeApprover::getNodeId, WorkflowNodeApprover::getSortOrder, WorkflowNodeApprover::getId))
                .stream()
                .collect(Collectors.groupingBy(WorkflowNodeApprover::getNodeId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<WorkflowNodeApproverDeptExpand>> deptExpandByApproverId = workflowNodeApproverDeptExpandMapper.selectList(
                        new LambdaQueryWrapper<WorkflowNodeApproverDeptExpand>()
                                .eq(WorkflowNodeApproverDeptExpand::getDefinitionId, definitionId)
                                .orderByAsc(WorkflowNodeApproverDeptExpand::getApproverId, WorkflowNodeApproverDeptExpand::getDistance, WorkflowNodeApproverDeptExpand::getId))
                .stream()
                .collect(Collectors.groupingBy(WorkflowNodeApproverDeptExpand::getApproverId, LinkedHashMap::new, Collectors.toList()));
        return new RuntimeDefinitionContext(startNodeId, nodeMap, transitionsByFromNodeId, approversByNodeId, deptExpandByApproverId);
    }

    /**
     * 构建申请人运行上下文，包含申请人及其主组织信息。
     */
    private RuntimeApplicantContext buildApplicantContext(BizApply bizApply) {
        User applicant = userMapper.selectById(bizApply.getApplicantId());
        if (Objects.isNull(applicant) || !Objects.equals(applicant.getStatus(), CommonStatusEnum.ENABLED.getId())) {
            throw new BizException("申请人不存在或已停用");
        }
        UserDeptRel primaryDeptRel = userDeptRelMapper.selectOne(new LambdaQueryWrapper<UserDeptRel>()
                .eq(UserDeptRel::getUserId, bizApply.getApplicantId())
                .eq(UserDeptRel::getIsPrimary, YesNoEnum.YES.getId())
                .last("LIMIT 1"));
        UserDept primaryDept = null;
        if (Objects.nonNull(primaryDeptRel) && Objects.nonNull(primaryDeptRel.getDeptId())) {
            primaryDept = userDeptMapper.selectById(primaryDeptRel.getDeptId());
        }
        return new RuntimeApplicantContext(applicant, primaryDeptRel, primaryDept);
    }

    /**
     * 基于业务申请和流程定义创建流程实例。
     */
    private WorkflowInstance buildWorkflowInstance(BizApply bizApply, WorkflowDefinition workflowDefinition) {
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setBizId(bizApply.getBizDefinitionId());
        workflowInstance.setDefinitionId(workflowDefinition.getId());
        workflowInstance.setDefinitionCode(workflowDefinition.getCode());
        workflowInstance.setTitle(bizApply.getTitle());
        workflowInstance.setStatus(WorkflowInstanceStatusEnum.RUNNING.getCode());
        workflowInstance.setApplicantId(bizApply.getApplicantId());
        workflowInstance.setApplicantName(bizApply.getApplicantName());
        workflowInstance.setFormData(bizApply.getFormData());
        workflowInstance.setStartedAt(LocalDateTime.now());
        return workflowInstance;
    }

    /**
     * 从开始节点出发激活首批目标节点。
     */
    private ActivationResult activateFromStartNode(Long startNodeId,
                                                   RuntimeDefinitionContext definitionContext,
                                                   WorkflowInstance workflowInstance,
                                                   BizApply bizApply,
                                                   RuntimeApplicantContext applicantContext,
                                                   List<WorkflowApprovalRecord> systemRecords) {
        // 查找开始节点
        List<WorkflowTransition> startTransitions = definitionContext.transitionsByFromNodeId()
                .getOrDefault(startNodeId, Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(WorkflowTransition::getPriority, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(WorkflowTransition::getId))
                .toList();
        return activateTargets(startTransitions, definitionContext, workflowInstance, bizApply, applicantContext, systemRecords, null);
    }

    /**
     * 按已解析出的连线集合依次激活目标节点。
     */
    private ActivationResult activateTargets(List<WorkflowTransition> transitions,
                                             RuntimeDefinitionContext definitionContext,
                                             WorkflowInstance workflowInstance,
                                             BizApply bizApply,
                                             RuntimeApplicantContext applicantContext,
                                             List<WorkflowApprovalRecord> systemRecords,
                                             WorkflowNodeInstance sourceNodeInstance) {
        WorkflowTransition firstTransition = transitions.get(0);
        ActivationResult firstResult = activateTargetNode(
                definitionContext.nodeMap().get(firstTransition.getToNodeId()),
                definitionContext,
                workflowInstance,
                bizApply,
                applicantContext,
                systemRecords,
                sourceNodeInstance
        );
        boolean finished = firstResult.finished();
        for (int index = 1; index < transitions.size(); index++) {
            WorkflowTransition transition = transitions.get(index);
            WorkflowNode targetNode = definitionContext.nodeMap().get(transition.getToNodeId());
            ActivationResult currentResult = activateTargetNode(targetNode, definitionContext, workflowInstance, bizApply, applicantContext, systemRecords, sourceNodeInstance);
            finished = finished || currentResult.finished();
        }
        if (transitions.size() > 1) {
            return new ActivationResult(sourceNodeInstance == null ? firstResult.currentNodeId() : sourceNodeInstance.getDefinitionNodeId(),
                    sourceNodeInstance == null ? firstResult.currentNodeName() : sourceNodeInstance.getDefinitionNodeName(),
                    sourceNodeInstance == null ? firstResult.currentNodeType() : sourceNodeInstance.getDefinitionNodeType(),
                    firstResult.currentNodeInstance(), firstResult.currentApproverInstances(), finished);
        }
        return new ActivationResult(firstResult.currentNodeId(), firstResult.currentNodeName(), firstResult.currentNodeType(),
                firstResult.currentNodeInstance(), firstResult.currentApproverInstances(), finished);
    }

    /**
     * 激活单个目标节点，并根据节点类型决定创建何种运行时数据。
     */
    private ActivationResult activateTargetNode(WorkflowNode targetNode,
                                                RuntimeDefinitionContext definitionContext,
                                                WorkflowInstance workflowInstance,
                                                BizApply bizApply,
                                                RuntimeApplicantContext applicantContext,
                                                List<WorkflowApprovalRecord> systemRecords,
                                                WorkflowNodeInstance sourceNodeInstance) {
        if (WorkflowNodeTypeEnum.END.getCode().equals(targetNode.getNodeType())) {
            return new ActivationResult(null, WorkflowNodeTypeEnum.END.getName(), WorkflowNodeTypeEnum.END.getCode(), sourceNodeInstance, Collections.emptyList(), true);
        }
        if (WorkflowNodeTypeEnum.APPROVAL.getCode().equals(targetNode.getNodeType())) {
            WorkflowNodeInstance nodeInstance = createApprovalNodeInstance(workflowInstance.getId(), targetNode, sourceNodeInstance);
            workflowNodeInstanceService.save(nodeInstance);
            List<WorkflowNodeApproverInstance> approverInstances = buildApproverInstances(
                    workflowInstance.getId(),
                    targetNode,
                    nodeInstance,
                    definitionContext.approversByNodeId().getOrDefault(targetNode.getId(), Collections.emptyList()),
                    definitionContext.deptExpandByApproverId(),
                    applicantContext
            );
            workflowNodeApproverInstanceService.saveBatch(approverInstances);
            return new ActivationResult(targetNode.getId(), targetNode.getName(), targetNode.getNodeType(),
                    nodeInstance, approverInstances, false);
        }
        if (WorkflowNodeTypeEnum.CONDITION.getCode().equals(targetNode.getNodeType())) {
            List<WorkflowTransition> nextTransitions = resolveNextTransitions(targetNode, workflowInstance, applicantContext, definitionContext);
            WorkflowNodeInstance conditionInstance = createInstantNodeInstance(workflowInstance.getId(), targetNode, sourceNodeInstance);
            workflowNodeInstanceService.save(conditionInstance);
            ActivationResult result = activateTargets(nextTransitions, definitionContext, workflowInstance, bizApply, applicantContext, systemRecords, conditionInstance);
            systemRecords.add(buildSystemRecord(workflowInstance.getId(), WorkflowApprovalActionEnum.ROUTE.getCode(),
                    conditionInstance, conditionInstance, result.currentNodeInstance(), null));
            return new ActivationResult(result.currentNodeId(), result.currentNodeName(), result.currentNodeType(),
                    result.currentNodeInstance(), result.currentApproverInstances(), result.finished());
        }
        if (WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode().equals(targetNode.getNodeType())) {
            List<WorkflowTransition> nextTransitions = resolveNextTransitions(targetNode, workflowInstance, applicantContext, definitionContext);
            WorkflowNodeInstance splitInstance = createInstantNodeInstance(workflowInstance.getId(), targetNode, sourceNodeInstance);
            workflowNodeInstanceService.save(splitInstance);
            ActivationResult result = activateTargets(nextTransitions, definitionContext, workflowInstance, bizApply, applicantContext, systemRecords, splitInstance);
            systemRecords.add(buildSystemRecord(workflowInstance.getId(), WorkflowApprovalActionEnum.SPLIT_TRIGGER.getCode(),
                    splitInstance, splitInstance, result.currentNodeInstance(), null));
            return new ActivationResult(targetNode.getId(), targetNode.getName(), targetNode.getNodeType(),
                    result.currentNodeInstance(), result.currentApproverInstances(), result.finished());
        }
        if (WorkflowNodeTypeEnum.PARALLEL_JOIN.getCode().equals(targetNode.getNodeType())) {
            List<WorkflowTransition> nextTransitions = resolveNextTransitions(targetNode, workflowInstance, applicantContext, definitionContext);
            return activateTargets(nextTransitions, definitionContext, workflowInstance, bizApply, applicantContext, systemRecords, null);
        }
        throw new BizException("暂不支持的节点类型：" + targetNode.getNodeType());
    }

    /**
     * 按节点类型解析当前节点允许执行的下一批连线。
     */
    private List<WorkflowTransition> resolveNextTransitions(WorkflowNode currentNode,
                                                            WorkflowInstance workflowInstance,
                                                            RuntimeApplicantContext applicantContext,
                                                            RuntimeDefinitionContext definitionContext) {
        List<WorkflowTransition> transitions = definitionContext.transitionsByFromNodeId()
                .getOrDefault(currentNode.getId(), Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(WorkflowTransition::getPriority, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(WorkflowTransition::getId))
                .toList();
        if (CollectionUtils.isEmpty(transitions)) {
            return Collections.emptyList();
        }
        if (WorkflowNodeTypeEnum.CONDITION.getCode().equals(currentNode.getNodeType())) {
            return resolveConditionTransitions(transitions, workflowInstance, applicantContext);
        }
        if (WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode().equals(currentNode.getNodeType())) {
            return resolveParallelSplitTransitions(transitions, workflowInstance, applicantContext);
        }
        return List.of(resolveSingleTransition(transitions, workflowInstance, applicantContext));
    }

    /**
     * 条件节点按优先级命中第一条分支，未命中时走默认分支。
     */
    private List<WorkflowTransition> resolveConditionTransitions(List<WorkflowTransition> transitions,
                                                                 WorkflowInstance workflowInstance,
                                                                 RuntimeApplicantContext applicantContext) {
        for (WorkflowTransition transition : transitions) {
            if (isDefaultTransition(transition)) {
                continue;
            }
            if (matchesTransition(transition, workflowInstance, applicantContext)) {
                return List.of(transition);
            }
        }
        return List.of(findDefaultTransition(transitions));
    }

    /**
     * 并行拆分节点激活全部命中分支，未命中时走默认分支。
     */
    private List<WorkflowTransition> resolveParallelSplitTransitions(List<WorkflowTransition> transitions,
                                                                     WorkflowInstance workflowInstance,
                                                                     RuntimeApplicantContext applicantContext) {
        List<WorkflowTransition> matchedTransitions = transitions.stream()
                .filter(transition -> !isDefaultTransition(transition))
                .filter(transition -> matchesTransition(transition, workflowInstance, applicantContext))
                .toList();
        if (!CollectionUtils.isEmpty(matchedTransitions)) {
            return matchedTransitions;
        }
        return List.of(findDefaultTransition(transitions));
    }

    /**
     * 普通串行节点解析唯一可执行连线。
     */
    private WorkflowTransition resolveSingleTransition(List<WorkflowTransition> transitions,
                                                       WorkflowInstance workflowInstance,
                                                       RuntimeApplicantContext applicantContext) {
        for (WorkflowTransition transition : transitions) {
            if (isDefaultTransition(transition)) {
                continue;
            }
            if (matchesTransition(transition, workflowInstance, applicantContext)) {
                return transition;
            }
        }
        return findDefaultTransition(transitions);
    }

    /**
     * 查询默认连线。
     */
    private WorkflowTransition findDefaultTransition(List<WorkflowTransition> transitions) {
        return transitions.stream()
                .filter(this::isDefaultTransition)
                .findFirst()
                .orElse(transitions.get(0));
    }

    /**
     * 判断连线是否为默认分支。
     */
    private boolean isDefaultTransition(WorkflowTransition transition) {
        return Objects.equals(transition.getIsDefault(), YesNoEnum.YES.getId());
    }

    /**
     * 基于表单数据和申请人上下文判断连线条件是否命中。
     */
    private boolean matchesTransition(WorkflowTransition transition,
                                      WorkflowInstance workflowInstance,
                                      RuntimeApplicantContext applicantContext) {
        if (!StringUtils.hasText(transition.getConditionExpr())) {
            return true;
        }
        Map<String, Object> variables = buildConditionVariables(workflowInstance, applicantContext);
        String normalizedExpression = normalizeConditionExpression(transition.getConditionExpr());
        try {
            StandardEvaluationContext context = new StandardEvaluationContext(variables);
            context.addPropertyAccessor(new MapAccessor());
            Boolean result = expressionParser.parseExpression(normalizedExpression).getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception ex) {
            throw new BizException("条件表达式求值失败");
        }
    }

    /**
     * 构建条件表达式求值变量。
     */
    private Map<String, Object> buildConditionVariables(WorkflowInstance workflowInstance,
                                                        RuntimeApplicantContext applicantContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (StringUtils.hasText(workflowInstance.getFormData())) {
            variables.putAll(objectMapperUtils.fromJson(workflowInstance.getFormData(), new TypeReference<Map<String, Object>>() {
            }));
        }
        variables.put("applicant_id", workflowInstance.getApplicantId());
        if (Objects.nonNull(applicantContext.primaryDept())) {
            variables.put("applicant_dept_id", applicantContext.primaryDept().getId());
            variables.put("applicant_dept_code", applicantContext.primaryDept().getCode());
            variables.put("department", applicantContext.primaryDept().getCode());
            variables.put("dept_code", applicantContext.primaryDept().getCode());
        }
        variables.put("current_time", LocalDateTime.now());
        return variables;
    }

    /**
     * 归一化前端表达式写法，转换为 SpEL 可执行格式。
     */
    private String normalizeConditionExpression(String expression) {
        return expression
                .replace("!==", "!=")
                .replace("===", "==")
                .replace("\"", "'");
    }

    /**
     * 创建瞬时网关节点实例，创建即完成。
     */
    private WorkflowNodeInstance createInstantNodeInstance(Long workflowInstanceId,
                                                           WorkflowNode node,
                                                           WorkflowNodeInstance sourceNodeInstance) {
        WorkflowNodeInstance nodeInstance = buildBaseNodeInstance(workflowInstanceId, node, sourceNodeInstance);
        LocalDateTime now = LocalDateTime.now();
        nodeInstance.setStatus(WorkflowNodeInstanceStatusEnum.APPROVED.getCode());
        nodeInstance.setActivatedAt(now);
        nodeInstance.setFinishedAt(now);
        return nodeInstance;
    }

    /**
     * 创建审批节点实例，并计算提醒时间与超时时间。
     */
    private WorkflowNodeInstance createApprovalNodeInstance(Long workflowInstanceId,
                                                            WorkflowNode node,
                                                            WorkflowNodeInstance sourceNodeInstance) {
        WorkflowNodeInstance nodeInstance = buildBaseNodeInstance(workflowInstanceId, node, sourceNodeInstance);
        LocalDateTime now = LocalDateTime.now();
        nodeInstance.setStatus(WorkflowNodeInstanceStatusEnum.ACTIVE.getCode());
        nodeInstance.setActivatedAt(now);
        if (Objects.nonNull(node.getTimeoutMinutes())) {
            nodeInstance.setDeadlineAt(now.plusMinutes(node.getTimeoutMinutes()));
        }
        if (Objects.nonNull(node.getRemindMinutes())) {
            nodeInstance.setRemindAt(now.plusMinutes(node.getRemindMinutes()));
        }
        return nodeInstance;
    }

    /**
     * 构建节点实例公共快照字段。
     */
    private WorkflowNodeInstance buildBaseNodeInstance(Long workflowInstanceId,
                                                       WorkflowNode node,
                                                       WorkflowNodeInstance sourceNodeInstance) {
        WorkflowNodeInstance nodeInstance = new WorkflowNodeInstance();
        nodeInstance.setInstanceId(workflowInstanceId);
        nodeInstance.setDefinitionNodeId(node.getId());
        nodeInstance.setDefinitionNodeName(node.getName());
        nodeInstance.setDefinitionNodeType(node.getNodeType());
        nodeInstance.setParallelBranchRootId(resolveParallelBranchRootId(node, sourceNodeInstance));
        nodeInstance.setApproveMode(node.getApproveMode());
        nodeInstance.setIsReminded(YesNoEnum.NO.getId());
        return nodeInstance;
    }

    /**
     * 解析节点实例所属并行分支根节点实例ID。
     */
    private Long resolveParallelBranchRootId(WorkflowNode targetNode, WorkflowNodeInstance sourceNodeInstance) {
        if (Objects.isNull(sourceNodeInstance)) {
            return null;
        }
        if (WorkflowNodeTypeEnum.PARALLEL_JOIN.getCode().equals(targetNode.getNodeType())) {
            return null;
        }
        if (WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode().equals(sourceNodeInstance.getDefinitionNodeType())) {
            return sourceNodeInstance.getId();
        }
        return sourceNodeInstance.getParallelBranchRootId();
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
    private List<ResolvedApprover> resolveUserApprovers(String approverValue) {
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
    private List<ResolvedApprover> resolveRoleApprovers(String approverValue) {
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
    private Long parseLongValue(String value, String message) {
        try {
            return Long.valueOf(value);
        } catch (Exception ex) {
            throw new BizException(message);
        }
    }

    /**
     * 构建提交动作审批记录。
     */
    private WorkflowApprovalRecord buildSubmitRecord(Long workflowInstanceId,
                                                     WorkflowNodeInstance firstNodeInstance,
                                                     User applicant) {
        if (Objects.isNull(firstNodeInstance)) {
            throw new BizException("流程未生成首个运行节点");
        }
        WorkflowApprovalRecord record = new WorkflowApprovalRecord();
        record.setInstanceId(workflowInstanceId);
        record.setNodeInstanceId(firstNodeInstance.getId());
        record.setOperatorId(applicant.getId());
        record.setOperatorName(resolveUserName(applicant));
        record.setAction(WorkflowApprovalActionEnum.SUBMIT.getCode());
        record.setNodeInstanceType(firstNodeInstance.getDefinitionNodeType());
        record.setNodeInstanceName(firstNodeInstance.getDefinitionNodeName());
        return record;
    }

    /**
     * 构建当前运行节点返回结果。
     */
    private WorkflowStartCurrentNodeResult buildCurrentNodeResult(WorkflowNodeInstance nodeInstance,
                                                                  List<WorkflowNodeApproverInstance> approverInstances) {
        if (Objects.isNull(nodeInstance)) {
            return null;
        }
        List<WorkflowStartApproverResult> approverList = Objects.nonNull(approverInstances)
                ? approverInstances.stream()
                .map(this::buildCurrentApproverResult)
                .toList()
                : Collections.emptyList();
        return new WorkflowStartCurrentNodeResult(
                nodeInstance.getId(),
                nodeInstance.getDefinitionNodeId(),
                nodeInstance.getDefinitionNodeName(),
                nodeInstance.getDefinitionNodeType(),
                nodeInstance.getStatus(),
                nodeInstance.getApproveMode(),
                approverList
        );
    }

    /**
     * 构建当前节点审核人返回结果。
     */
    private WorkflowStartApproverResult buildCurrentApproverResult(WorkflowNodeApproverInstance approverInstance) {
        return new WorkflowStartApproverResult(
                approverInstance.getApproverId(),
                approverInstance.getApproverName(),
                approverInstance.getStatus(),
                approverInstance.getIsActive(),
                approverInstance.getSortOrder(),
                approverInstance.getRelationType()
        );
    }

    /**
     * 构建系统自动动作审批记录。
     */
    private WorkflowApprovalRecord buildSystemRecord(Long workflowInstanceId,
                                                     String action,
                                                     WorkflowNodeInstance nodeInstance,
                                                     WorkflowNodeInstance fromNodeInstance,
                                                     WorkflowNodeInstance toNodeInstance,
                                                     String comment) {
        WorkflowApprovalRecord record = new WorkflowApprovalRecord();
        record.setInstanceId(workflowInstanceId);
        record.setNodeInstanceId(nodeInstance.getId());
        record.setOperatorId(WorkflowRuntimeConstants.SYSTEM_OPERATOR_ID);
        record.setOperatorName(WorkflowRuntimeConstants.SYSTEM_OPERATOR_NAME);
        record.setAction(action);
        record.setNodeInstanceType(nodeInstance.getDefinitionNodeType());
        record.setNodeInstanceName(nodeInstance.getDefinitionNodeName());
        if (Objects.nonNull(fromNodeInstance)) {
            record.setFromNodeId(fromNodeInstance.getId());
            record.setFromNodeType(fromNodeInstance.getDefinitionNodeType());
            record.setFromNodeName(fromNodeInstance.getDefinitionNodeName());
        }
        if (Objects.nonNull(toNodeInstance)) {
            record.setToNodeId(toNodeInstance.getId());
            record.setToNodeType(toNodeInstance.getDefinitionNodeType());
            record.setToNodeName(toNodeInstance.getDefinitionNodeName());
        }
        record.setComment(comment);
        return record;
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

        // 查找出边
        ParallelJoinContext joinContext = buildParallelJoinContext(context);
        WorkflowNode joinNode = findNextParallelJoinNode(
                context.currentNodeInstance().getDefinitionNodeId(),
                joinContext.nodeMap(),
                joinContext.transitions()
        );
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

        ParallelJoinContext parallelJoinContext = buildParallelJoinContext(context);
        WorkflowNode nextParallelJoinNode = findNextParallelJoinNode(
                context.currentNodeInstance().getDefinitionNodeId(),
                parallelJoinContext.nodeMap(),
                parallelJoinContext.transitions()
        );
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
            workflowInstanceService.updateNodeForReject(context.workflowInstance().getId());
            workflowApprovalRecordService.insertRecordForReject(context.eto(), joinNodeInstance, WorkflowNodeInstance.toEndWorkflowNodeInstance());
            return;
        }
        workflowNodeInstanceService.updateNodeForApprove(joinNodeInstance.getId(), "并行分支全部通过");

        // Join节点通过后，后续流转
        continueAfterParallelJoinApproved(context, joinNodeInstance);
    }

    private void continueAfterParallelJoinApproved(AuditContext context, WorkflowNodeInstance joinNodeInstance) {
        ParallelJoinContext joinContext = buildParallelJoinContext(context);
        List<WorkflowTransition> outgoingTransitions = joinContext.transitions()
                .getOrDefault(joinNodeInstance.getDefinitionNodeId(), Collections.emptyList());
        if (CollectionUtils.isEmpty(outgoingTransitions)) {
            throw new BizException("并行聚合节点缺少后续连线");
        }

        WorkflowTransition transition = outgoingTransitions.get(0);
        WorkflowNode targetNode = joinContext.nodeMap().get(transition.getToNodeId());
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

    private WorkflowNode findParallelJoinNode(Long splitDefinitionNodeId,
                                              Map<Long, WorkflowNode> nodeMap,
                                              Map<Long, List<WorkflowTransition>> transitionMap) {
        Deque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(splitDefinitionNodeId);

        while (!queue.isEmpty()) {
            Long currentNodeId = queue.poll();
            if (!visited.add(currentNodeId)) {
                continue;
            }

            for (WorkflowTransition transition : transitionMap.getOrDefault(currentNodeId, Collections.emptyList())) {
                WorkflowNode targetNode = nodeMap.get(transition.getToNodeId());
                if (targetNode == null) {
                    continue;
                }

                if (WorkflowNodeTypeEnum.isParallelJoin(targetNode.getNodeType())) {
                    return targetNode;
                }

                queue.add(targetNode.getId());
            }
        }

        throw new BizException("未找到并行聚合节点");
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
        workflowInstanceService.updateNodeForReject(instanceId);
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
        workflowInstanceService.updateNodeForReject(context.workflowInstance().getId());
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
                        .eq(WorkflowApprovalRecord::getNodeInstanceId, context.currentNodeInstance.getId())
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

        List<WorkflowNodeApproverInstance> approverInstances = buildApproverInstances(targetNode, nextNodeInstance);
        workflowNodeApproverInstanceService.saveBatch(approverInstances);

        WorkflowInstance updateInstance = new WorkflowInstance();
        updateInstance.setId(context.workflowInstance().getId());
        updateInstance.setCurrentNodeId(nextNodeInstance.getId());
        updateInstance.setCurrentNodeName(nextNodeInstance.getDefinitionNodeName());
        updateInstance.setCurrentNodeType(nextNodeInstance.getDefinitionNodeType());
        workflowInstanceService.updateById(updateInstance);
    }

    private ParallelJoinContext buildParallelJoinContext(AuditContext context) {


        Map<Long, WorkflowNode> nodeMap = context.definitionWorkflowNodes().stream()
                .collect(Collectors.toMap(WorkflowNode::getId, item -> item));

        Map<Long, List<WorkflowTransition>> transitionsByFromNodeId = context.definitionWorkflowTransitions().stream()
                .collect(Collectors.groupingBy(
                        WorkflowTransition::getFromNodeId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return new ParallelJoinContext(nodeMap, transitionsByFromNodeId);
    }

    /**
     * <p>当前方法用于并行分支执行完成后的收口处理，查找规则基于以下约束：</p>
     * <p>1. 当前场景只处理“一个并行拆分节点对应一个并行聚合节点”的常规并行结构。</p>
     * <p>2. 暂不考虑并行结构嵌套，也不处理一个分支再次进入新的并行拆分节点的情况。</p>
     * <p>3. 因此，只要从当前分支节点开始，顺着定义层出边持续向后遍历，
     */
    private WorkflowNode findNextParallelJoinNode(Long currentDefinitionNodeId,
                                                  Map<Long, WorkflowNode> nodeMap,
                                                  Map<Long, List<WorkflowTransition>> transitionsByFromNodeId) {
        Deque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(currentDefinitionNodeId);

        while (!queue.isEmpty()) {
            Long nodeId = queue.poll();
            if (!visited.add(nodeId)) {
                continue;
            }

            List<WorkflowTransition> outgoingList =
                    transitionsByFromNodeId.getOrDefault(nodeId, Collections.emptyList());

            for (WorkflowTransition transition : outgoingList) {
                WorkflowNode targetNode = nodeMap.get(transition.getToNodeId());
                if (targetNode == null) {
                    continue;
                }

                if (WorkflowNodeTypeEnum.PARALLEL_JOIN.getCode().equals(targetNode.getNodeType())) {
                    return targetNode;
                }

                queue.add(targetNode.getId());
            }
        }
        throw new BizException("未找到并行聚合节点");
    }


    private record RuntimeDefinitionContext(
            Long startNodeId,
            Map<Long, WorkflowNode> nodeMap,
            Map<Long, List<WorkflowTransition>> transitionsByFromNodeId,
            Map<Long, List<WorkflowNodeApprover>> approversByNodeId,
            Map<Long, List<WorkflowNodeApproverDeptExpand>> deptExpandByApproverId
    ) {
    }

    private record WorkflowStartPreparation(
            BizApply bizApply,
            WorkflowDefinition workflowDefinition,
            RuntimeDefinitionContext definitionContext,
            RuntimeApplicantContext applicantContext
    ) {
    }

    private record WorkflowStartExecution(
            ActivationResult activationResult,
            List<WorkflowApprovalRecord> systemRecords
    ) {
    }

    private record RuntimeApplicantContext(
            User applicant,
            UserDeptRel primaryDeptRel,
            UserDept primaryDept
    ) {
    }

    private record ActivationResult(
            Long currentNodeId,
            String currentNodeName,
            String currentNodeType,
            WorkflowNodeInstance currentNodeInstance,
            List<WorkflowNodeApproverInstance> currentApproverInstances,
            boolean finished
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

    private record ParallelJoinContext(
            Map<Long, WorkflowNode> nodeMap,
            Map<Long, List<WorkflowTransition>> transitions
    ) {

    }
}
