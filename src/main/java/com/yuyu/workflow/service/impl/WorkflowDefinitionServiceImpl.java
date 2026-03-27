package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.enums.WorkflowApproveModeEnum;
import com.yuyu.workflow.common.enums.WorkflowApproverTypeEnum;
import com.yuyu.workflow.common.enums.WorkflowDefinitionStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.common.enums.WorkflowTimeoutActionEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.common.util.ObjectMapperUtils;
import com.yuyu.workflow.convert.WorkflowDefinitionStructMapper;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionCreateETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionDisableETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionNodeETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionPublishETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionUpdateETO;
import com.yuyu.workflow.eto.workflow.WorkflowNodeApproverETO;
import com.yuyu.workflow.eto.workflow.WorkflowTransitionETO;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.WorkflowDefinitionMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import com.yuyu.workflow.mapper.WorkflowTransitionMapper;
import com.yuyu.workflow.qto.workflow.WorkflowDefinitionListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowDefinitionPageQTO;
import com.yuyu.workflow.service.WorkflowDefinitionService;
import com.yuyu.workflow.service.WorkflowNodeApproverService;
import com.yuyu.workflow.service.WorkflowNodeApproverDeptExpandService;
import com.yuyu.workflow.vo.workflow.WorkflowDefinitionVO;
import com.yuyu.workflow.vo.workflow.WorkflowNodeApproverVO;
import com.yuyu.workflow.vo.workflow.WorkflowNodeVO;
import com.yuyu.workflow.vo.workflow.WorkflowTransitionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现。
 */
@Service
public class WorkflowDefinitionServiceImpl extends ServiceImpl<WorkflowDefinitionMapper, WorkflowDefinition> implements WorkflowDefinitionService {

    private static final String FRONT_NODE_ROLE_START_END = "START_END";
    private static final String FRONT_APPROVE_MODE_COUNTERSIGN = "COUNTERSIGN";
    private static final String FRONT_APPROVE_MODE_OR_SIGN = "OR_SIGN";
    private static final String FRONT_TIMEOUT_ACTION_REMIND_ONLY = "REMIND_ONLY";
    private static final String FRONT_TIMEOUT_ACTION_AUTO_PASS = "AUTO_PASS";
    private static final Integer DEFAULT_BRANCH_NO = 0;
    private static final Integer DEFAULT_BRANCH_YES = 1;

    private final WorkflowDefinitionMapper workflowDefinitionMapper;
    private final WorkflowNodeMapper workflowNodeMapper;
    private final WorkflowNodeApproverMapper workflowNodeApproverMapper;
    private final WorkflowNodeApproverService workflowNodeApproverService;
    private final WorkflowTransitionMapper workflowTransitionMapper;
    private final WorkflowDefinitionStructMapper workflowDefinitionStructMapper;
    private final WorkflowNodeApproverDeptExpandService workflowNodeApproverDeptExpandService;
    private final UserDeptMapper userDeptMapper;
    private final ObjectMapperUtils objectMapperUtils;

    /**
     * 注入流程定义模块依赖。
     */
    public WorkflowDefinitionServiceImpl(WorkflowDefinitionMapper workflowDefinitionMapper,
                                         WorkflowNodeMapper workflowNodeMapper,
                                         WorkflowNodeApproverMapper workflowNodeApproverMapper,
                                         WorkflowNodeApproverService workflowNodeApproverService,
                                         WorkflowTransitionMapper workflowTransitionMapper,
                                         WorkflowDefinitionStructMapper workflowDefinitionStructMapper,
                                         WorkflowNodeApproverDeptExpandService workflowNodeApproverDeptExpandService,
                                         UserDeptMapper userDeptMapper,
                                         ObjectMapperUtils objectMapperUtils) {
        this.baseMapper = workflowDefinitionMapper;
        this.workflowDefinitionMapper = workflowDefinitionMapper;
        this.workflowNodeMapper = workflowNodeMapper;
        this.workflowNodeApproverMapper = workflowNodeApproverMapper;
        this.workflowNodeApproverService = workflowNodeApproverService;
        this.workflowTransitionMapper = workflowTransitionMapper;
        this.workflowDefinitionStructMapper = workflowDefinitionStructMapper;
        this.workflowNodeApproverDeptExpandService = workflowNodeApproverDeptExpandService;
        this.userDeptMapper = userDeptMapper;
        this.objectMapperUtils = objectMapperUtils;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionVO create(WorkflowDefinitionCreateETO eto) {
        ParsedWorkflow parsedWorkflow = parseWorkflowPayload(eto.getWorkFlowJson());
        validateDefinitionPayload(parsedWorkflow.nodes(), parsedWorkflow.transitions());
        assertNoDraftVersion(eto.getCode());

        WorkflowDefinition entity = workflowDefinitionStructMapper.toEntity(eto);
        entity.setVersion(resolveNextVersion(eto.getCode()));
        entity.setStatus(WorkflowDefinitionStatusEnum.DRAFT.getId());
        entity.setCreatedBy(requireCurrentUserId(eto.getCurrentUserId()));
        workflowDefinitionMapper.insert(entity);
        persistChildren(entity.getId(), parsedWorkflow.nodes(), parsedWorkflow.transitions());
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionVO update(WorkflowDefinitionUpdateETO eto) {
        WorkflowDefinition oldEntity = getDefinitionOrThrow(eto.getId());
        ParsedWorkflow parsedWorkflow = parseWorkflowPayload(eto.getWorkFlowJson());
        validateDefinitionPayload(parsedWorkflow.nodes(), parsedWorkflow.transitions());
        if (WorkflowDefinitionStatusEnum.DRAFT.getId().equals(oldEntity.getStatus())) {
            WorkflowDefinition entity = workflowDefinitionStructMapper.toUpdatedEntity(eto, oldEntity);
            workflowDefinitionMapper.updateById(entity);
            deleteChildren(List.of(entity.getId()));
            persistChildren(entity.getId(), parsedWorkflow.nodes(), parsedWorkflow.transitions());
            return detail(entity.getId());
        }

        assertNoDraftVersion(oldEntity.getCode());
        WorkflowDefinition entity = buildNextDraftVersionEntity(eto, oldEntity);
        workflowDefinitionMapper.insert(entity);
        persistChildren(entity.getId(), parsedWorkflow.nodes(), parsedWorkflow.transitions());
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> idList) {
        List<Long> definitionIds = normalizeDeleteIds(idList);
        for (Long definitionId : definitionIds) {
            getDraftDefinitionOrThrow(definitionId);
        }
        deleteChildren(definitionIds);
        workflowDefinitionMapper.removeByIds(definitionIds);
    }

    @Override
    public List<WorkflowDefinitionVO> list(WorkflowDefinitionListQTO qto) {
        return buildSummaryVOList(workflowDefinitionMapper.selectList(
                buildDefinitionQuery(qto.getName(), qto.getCode(), qto.getStatus())));
    }

    @Override
    public PageVo<WorkflowDefinitionVO> page(WorkflowDefinitionPageQTO qto) {
        IPage<WorkflowDefinition> page = workflowDefinitionMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(qto.getPageNum(), qto.getPageSize()),
                buildDefinitionQuery(qto.getName(), qto.getCode(), qto.getStatus())
        );
        return PageVo.of(page.getCurrent(), page.getSize(), page.getTotal(), buildSummaryVOList(page.getRecords()));
    }

    @Override
    public WorkflowDefinitionVO detail(Long id) {
        WorkflowDefinition definition = getDefinitionOrThrow(id);
        WorkflowDefinitionVO vo = workflowDefinitionStructMapper.toTarget(definition);
        vo.setStatusMsg(WorkflowDefinitionStatusEnum.getMsgById(definition.getStatus()));

        List<WorkflowNode> nodeList = workflowNodeMapper.selectList(new LambdaQueryWrapper<WorkflowNode>()
                .eq(WorkflowNode::getDefinitionId, id)
                .orderByAsc(WorkflowNode::getId));
        Map<Long, List<WorkflowNodeApproverVO>> approverMap = buildApproverMap(
                nodeList.stream().map(WorkflowNode::getId).collect(Collectors.toCollection(LinkedHashSet::new))
        );

        List<WorkflowNodeVO> nodeVOList = new ArrayList<>();
        for (WorkflowNode node : nodeList) {
            WorkflowNodeVO nodeVO = workflowDefinitionStructMapper.toNodeVO(node);
            nodeVO.setNodeTypeMsg(WorkflowNodeTypeEnum.getMsgByCode(node.getNodeType()));
            nodeVO.setApproveModeMsg(WorkflowApproveModeEnum.getMsgByCode(node.getApproveMode()));
            nodeVO.setTimeoutActionMsg(WorkflowTimeoutActionEnum.getMsgByCode(node.getTimeoutAction()));
            nodeVO.setApproverList(approverMap.getOrDefault(node.getId(), Collections.emptyList()));
            nodeVOList.add(nodeVO);
        }
        vo.setNodeList(nodeVOList);

        List<WorkflowTransition> transitionList = workflowTransitionMapper.selectList(new LambdaQueryWrapper<WorkflowTransition>()
                .eq(WorkflowTransition::getDefinitionId, id)
                .orderByAsc(WorkflowTransition::getPriority, WorkflowTransition::getId));
        vo.setTransitionList(transitionList.stream()
                .map(workflowDefinitionStructMapper::toTransitionVO)
                .toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(WorkflowDefinitionPublishETO eto) {
        WorkflowDefinition definition = getDraftDefinitionOrThrow(eto.getId());
        validatePublishTransitionRules(definition.getId());
        List<WorkflowDefinition> publishedList = workflowDefinitionMapper.selectList(new LambdaQueryWrapper<WorkflowDefinition>()
                .eq(WorkflowDefinition::getCode, definition.getCode())
                .eq(WorkflowDefinition::getStatus, WorkflowDefinitionStatusEnum.PUBLISHED.getId()));
        for (WorkflowDefinition published : publishedList) {
            WorkflowDefinition disabledEntity = new WorkflowDefinition();
            disabledEntity.setId(published.getId());
            disabledEntity.setStatus(WorkflowDefinitionStatusEnum.DISABLED.getId());
            workflowDefinitionMapper.updateById(disabledEntity);
        }

        WorkflowDefinition publishedEntity = new WorkflowDefinition();
        publishedEntity.setId(definition.getId());
        publishedEntity.setStatus(WorkflowDefinitionStatusEnum.PUBLISHED.getId());
        workflowDefinitionMapper.updateById(publishedEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(WorkflowDefinitionDisableETO eto) {
        WorkflowDefinition definition = getDefinitionOrThrow(eto.getId());
        if (WorkflowDefinitionStatusEnum.DISABLED.getId().equals(definition.getStatus())) {
            return;
        }
        WorkflowDefinition disabledEntity = new WorkflowDefinition();
        disabledEntity.setId(definition.getId());
        disabledEntity.setStatus(WorkflowDefinitionStatusEnum.DISABLED.getId());
        workflowDefinitionMapper.updateById(disabledEntity);
    }

    /**
     * 统一构造定义查询条件。
     */
    private LambdaQueryWrapper<WorkflowDefinition> buildDefinitionQuery(String name,
                                                                        String code,
                                                                        Integer status) {
        return new LambdaQueryWrapper<WorkflowDefinition>()
                .like(StringUtils.hasText(name), WorkflowDefinition::getName, name)
                .like(StringUtils.hasText(code), WorkflowDefinition::getCode, code)
                .eq(Objects.nonNull(status), WorkflowDefinition::getStatus, status)
                .orderByDesc(WorkflowDefinition::getCode, WorkflowDefinition::getVersion);
    }

    /**
     * 汇总列表返回，不拼装子表明细。
     */
    private List<WorkflowDefinitionVO> buildSummaryVOList(List<WorkflowDefinition> definitionList) {
        if (CollectionUtils.isEmpty(definitionList)) {
            return Collections.emptyList();
        }
        return definitionList.stream().map(definition -> {
            WorkflowDefinitionVO vo = workflowDefinitionStructMapper.toTarget(definition);
            vo.setWorkFlowJson(null);
            vo.setStatusMsg(WorkflowDefinitionStatusEnum.getMsgById(definition.getStatus()));
            return vo;
        }).toList();
    }

    /**
     * 查询定义，不存在时抛出业务异常。
     */
    private WorkflowDefinition getDefinitionOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        WorkflowDefinition definition = workflowDefinitionMapper.selectById(id);
        if (Objects.isNull(definition)) {
            throw new BizException(RespCodeEnum.BIZ_ERROR.getId(), "流程定义不存在");
        }
        return definition;
    }

    /**
     * 查询草稿定义，不允许直接修改已发布或已停用版本。
     */
    private WorkflowDefinition getDraftDefinitionOrThrow(Long id) {
        WorkflowDefinition definition = getDefinitionOrThrow(id);
        if (!WorkflowDefinitionStatusEnum.DRAFT.getId().equals(definition.getStatus())) {
            throw new BizException("仅草稿流程允许修改");
        }
        return definition;
    }

    /**
     * 构造基于旧版本的新草稿版本实体。
     */
    private WorkflowDefinition buildNextDraftVersionEntity(WorkflowDefinitionUpdateETO eto, WorkflowDefinition oldEntity) {
        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setName(eto.getName());
        entity.setCode(oldEntity.getCode());
        entity.setVersion(resolveNextVersion(oldEntity.getCode()));
        entity.setDescription(eto.getDescription());
        entity.setWorkflowJson(eto.getWorkFlowJson());
        entity.setStatus(WorkflowDefinitionStatusEnum.DRAFT.getId());
        entity.setCreatedBy(requireCurrentUserId(eto.getCurrentUserId()));
        return entity;
    }

    /**
     * 确保当前操作人存在。
     */
    private Long requireCurrentUserId(Long currentUserId) {
        if (Objects.isNull(currentUserId)) {
            throw new BizException("currentUserId不能为空");
        }
        return currentUserId;
    }

    /**
     * 解析下一个版本号。
     */
    private Integer resolveNextVersion(String code) {
        Integer maxVersion = workflowDefinitionMapper.selectMaxVersionByCode(code);
        return Objects.isNull(maxVersion) ? 1 : maxVersion + 1;
    }

    /**
     * 同一流程编码只允许保留一个未发布草稿。
     */
    private void assertNoDraftVersion(String code) {
        WorkflowDefinition draft = workflowDefinitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinition>()
                .eq(WorkflowDefinition::getCode, code)
                .eq(WorkflowDefinition::getStatus, WorkflowDefinitionStatusEnum.DRAFT.getId())
                .last("limit 1"));
        if (Objects.nonNull(draft)) {
            throw new BizException("同一流程编码已存在未发布草稿");
        }
    }

    /**
     * 解析前端流程设计 JSON。
     */
    private ParsedWorkflow parseWorkflowPayload(String workFlowJson) {
        if (!StringUtils.hasText(workFlowJson)) {
            throw new BizException("workFlowJson不能为空");
        }
        WorkflowGraphPayload payload;
        try {
            payload = objectMapperUtils.fromJson(workFlowJson, WorkflowGraphPayload.class);
        } catch (IllegalArgumentException ex) {
            throw new BizException("workFlowJson格式不正确");
        }
        List<WorkflowGraphNode> rawNodeList = Objects.nonNull(payload) && !CollectionUtils.isEmpty(payload.nodes)
                ? payload.nodes
                : Collections.emptyList();
        List<WorkflowGraphEdge> rawEdgeList = Objects.nonNull(payload) && !CollectionUtils.isEmpty(payload.edges)
                ? payload.edges
                : Collections.emptyList();

        Map<String, Integer> inDegreeMap = new HashMap<>();
        Map<String, Integer> outDegreeMap = new HashMap<>();
        for (WorkflowGraphEdge edge : rawEdgeList) {
            String sourceNodeId = trimToNull(edge.sourceNodeId);
            String targetNodeId = trimToNull(edge.targetNodeId);
            if (StringUtils.hasText(sourceNodeId)) {
                outDegreeMap.merge(sourceNodeId, 1, Integer::sum);
            }
            if (StringUtils.hasText(targetNodeId)) {
                inDegreeMap.merge(targetNodeId, 1, Integer::sum);
            }
        }

        List<WorkflowDefinitionNodeETO> nodeList = new ArrayList<>();
        for (WorkflowGraphNode rawNode : rawNodeList) {
            Map<String, Object> properties = Objects.nonNull(rawNode.properties) ? rawNode.properties : Collections.emptyMap();
            String clientNodeId = trimToNull(rawNode.id);
            WorkflowDefinitionNodeETO node = new WorkflowDefinitionNodeETO();
            node.setCode(clientNodeId);
            node.setName(resolveNodeName(rawNode));
            node.setNodeType(resolveNodeType(clientNodeId,
                    node.getName(),
                    getString(properties, "nodeRole"),
                    inDegreeMap.getOrDefault(clientNodeId, 0),
                    outDegreeMap.getOrDefault(clientNodeId, 0)));
            node.setApproveMode(resolveApproveMode(getString(properties, "approveMode")));
            node.setTimeoutMinutes(getInteger(properties.get("timeoutAfterMinutes")));
            node.setTimeoutAction(resolveTimeoutAction(getString(properties, "timeoutStrategy")));
            node.setRemindMinutes(getInteger(properties.get("remindAfterMinutes")));
            node.setPositionX(getInteger(rawNode.x));
            node.setPositionY(getInteger(rawNode.y));
            node.setConfigJson(objectMapperUtils.toJson(properties));
            node.setApproverList(buildApproverList(getString(properties, "approverType"), properties.get("approverIds")));
            nodeList.add(node);
        }

        List<WorkflowTransitionETO> transitionList = new ArrayList<>();
        for (WorkflowGraphEdge rawEdge : rawEdgeList) {
            Map<String, Object> properties = Objects.nonNull(rawEdge.properties) ? rawEdge.properties : Collections.emptyMap();
            WorkflowTransitionETO transition = new WorkflowTransitionETO();
            transition.setFromNodeCode(trimToNull(rawEdge.sourceNodeId));
            transition.setToNodeCode(trimToNull(rawEdge.targetNodeId));
            transition.setConditionExpr(trimToNull(getString(properties, "expression")));
            transition.setIsDefault(getYesNoFlag(properties.get("is_default")));
            transition.setPriority(getInteger(properties.get("priority")));
            transition.setLabel(resolveEdgeLabel(rawEdge));
            transitionList.add(transition);
        }
        return new ParsedWorkflow(nodeList, transitionList);
    }

    /**
     * 校验流程定义提交结构。
     */
    private void validateDefinitionPayload(List<WorkflowDefinitionNodeETO> nodes, List<WorkflowTransitionETO> transitions) {
        if (CollectionUtils.isEmpty(nodes)) {
            throw new BizException("nodes不能为空");
        }
        if (CollectionUtils.isEmpty(transitions)) {
            throw new BizException("transitions不能为空");
        }
        Map<String, WorkflowDefinitionNodeETO> nodeMap = new LinkedHashMap<>();
        long startCount = 0;
        long endCount = 0;
        for (WorkflowDefinitionNodeETO node : nodes) {
            if (!StringUtils.hasText(node.getCode())) {
                throw new BizException("节点id不能为空");
            }
            if (nodeMap.putIfAbsent(node.getCode(), node) != null) {
                throw new BizException("节点id不能重复");
            }
            validateNodePayload(node);
            if (WorkflowNodeTypeEnum.START.getCode().equals(node.getNodeType())) {
                startCount++;
            }
            if (WorkflowNodeTypeEnum.END.getCode().equals(node.getNodeType())) {
                endCount++;
            }
        }
        if (startCount != 1) {
            throw new BizException("必须且只能存在一个开始节点");
        }
        if (endCount < 1) {
            throw new BizException("至少需要一个结束节点");
        }
        Map<String, List<String>> adjacencyMap = new HashMap<>();
        for (WorkflowTransitionETO transition : transitions) {
            if (!StringUtils.hasText(transition.getFromNodeCode()) || !StringUtils.hasText(transition.getToNodeCode())) {
                throw new BizException("连线必须配置来源和目标节点");
            }
            if (!nodeMap.containsKey(transition.getFromNodeCode()) || !nodeMap.containsKey(transition.getToNodeCode())) {
                throw new BizException("连线引用了不存在的节点");
            }
            if (Objects.equals(transition.getFromNodeCode(), transition.getToNodeCode())) {
                throw new BizException("连线不能指向自身");
            }
            adjacencyMap.computeIfAbsent(transition.getFromNodeCode(), key -> new ArrayList<>()).add(transition.getToNodeCode());
        }
        validateTransitionDefaultRule(nodeMap, transitions);
        String startCode = nodeMap.values().stream()
                .filter(item -> WorkflowNodeTypeEnum.START.getCode().equals(item.getNodeType()))
                .map(WorkflowDefinitionNodeETO::getCode)
                .findFirst()
                .orElseThrow(() -> new BizException("必须且只能存在一个开始节点"));
        validateReachability(startCode, adjacencyMap, nodeMap.keySet());
    }

    /**
     * 校验节点级规则。
     */
    private void validateNodePayload(WorkflowDefinitionNodeETO node) {
        if (!StringUtils.hasText(node.getName())) {
            throw new BizException("节点名称不能为空");
        }
        if (!StringUtils.hasText(node.getNodeType()) || !WorkflowNodeTypeEnum.containsCode(node.getNodeType())) {
            throw new BizException("nodeType不合法");
        }
        if (StringUtils.hasText(node.getTimeoutAction()) && !WorkflowTimeoutActionEnum.containsCode(node.getTimeoutAction())) {
            throw new BizException("timeoutAction不合法");
        }
        List<WorkflowNodeApproverETO> approverList = Objects.nonNull(node.getApproverList())
                ? node.getApproverList()
                : Collections.emptyList();
        if (WorkflowNodeTypeEnum.APPROVAL.getCode().equals(node.getNodeType())) {
            if (!StringUtils.hasText(node.getApproveMode())) {
                throw new BizException("审批节点必须配置approveMode");
            }
            if (!WorkflowApproveModeEnum.containsCode(node.getApproveMode())) {
                throw new BizException("approveMode不合法");
            }
            if (CollectionUtils.isEmpty(approverList)) {
                throw new BizException("审批节点必须配置审批人");
            }
            for (WorkflowNodeApproverETO approver : approverList) {
                validateApproverPayload(approver);
            }
            return;
        }
        if (!CollectionUtils.isEmpty(approverList)) {
            throw new BizException("非审批节点不能配置审批人");
        }
        if (StringUtils.hasText(node.getApproveMode())) {
            throw new BizException("非审批节点不能配置approveMode");
        }
    }

    /**
     * 校验审批人配置必须是一条记录一个审批主体，组织审批人必须指向有效组织。
     */
    private void validateApproverPayload(WorkflowNodeApproverETO approver) {
        if (!StringUtils.hasText(approver.getApproverType())) {
            throw new BizException("approverType不能为空");
        }
        if (!WorkflowApproverTypeEnum.containsCode(approver.getApproverType())) {
            throw new BizException("approverType不合法");
        }
        if (!StringUtils.hasText(approver.getApproverValue())) {
            throw new BizException("approverValue不能为空");
        }
        if (approver.getApproverValue().contains(",")) {
            throw new BizException("approverValue不允许使用逗号拼接多个值");
        }
        if (!WorkflowApproverTypeEnum.DEPT.getCode().equals(approver.getApproverType())) {
            return;
        }
        Long deptId = parseApproverDeptId(approver.getApproverValue());
        if (Objects.isNull(deptId)) {
            throw new BizException("组织审批人approverValue必须是单个组织ID");
        }
        UserDept dept = userDeptMapper.selectById(deptId);
        if (Objects.isNull(dept)) {
            throw new BizException("审批组织不存在");
        }
        if (!CommonStatusEnum.ENABLED.getId().equals(dept.getStatus())) {
            throw new BizException("审批组织已停用");
        }
    }

    /**
     * 校验所有节点都可从开始节点到达。
     */
    private void validateReachability(String startCode,
                                      Map<String, List<String>> adjacencyMap,
                                      Set<String> nodeCodes) {
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(startCode);
        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (!visited.add(current)) {
                continue;
            }
            for (String next : adjacencyMap.getOrDefault(current, Collections.emptyList())) {
                stack.push(next);
            }
        }
        if (visited.size() != nodeCodes.size()) {
            throw new BizException("存在不可达节点");
        }
    }

    /**
     * 持久化节点、审批人和连线。
     */
    private void persistChildren(Long definitionId,
                                 List<WorkflowDefinitionNodeETO> nodes,
                                 List<WorkflowTransitionETO> transitions) {
        Map<String, Long> nodeIdMap = new LinkedHashMap<>();
        Map<String, WorkflowDefinitionNodeETO> nodeConfigMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowDefinitionNodeETO::getCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<Long> deptApproverIds = new ArrayList<>();
        for (WorkflowDefinitionNodeETO nodeETO : nodes) {
            WorkflowNode node = workflowDefinitionStructMapper.toNodeEntity(nodeETO);
            node.setDefinitionId(definitionId);
            node.setPositionX(Objects.nonNull(nodeETO.getPositionX()) ? nodeETO.getPositionX() : 0);
            node.setPositionY(Objects.nonNull(nodeETO.getPositionY()) ? nodeETO.getPositionY() : 0);
            workflowNodeMapper.insert(node);
            nodeIdMap.put(nodeETO.getCode(), node.getId());

            List<WorkflowNodeApproverETO> approverList = Objects.nonNull(nodeETO.getApproverList())
                    ? nodeETO.getApproverList()
                    : Collections.emptyList();
            for (WorkflowNodeApproverETO approverETO : approverList) {
                WorkflowNodeApprover approver = workflowDefinitionStructMapper.toApproverEntity(approverETO);
                approver.setDefinitionId(definitionId);
                approver.setNodeId(node.getId());
                approver.setSortOrder(Objects.nonNull(approverETO.getSortOrder()) ? approverETO.getSortOrder() : 0);
                workflowNodeApproverService.save(approver);
                if (WorkflowApproverTypeEnum.DEPT.getCode().equals(approverETO.getApproverType())) {
                    deptApproverIds.add(approver.getId());
                }
            }
        }

        for (WorkflowTransitionETO transitionETO : transitions) {
            WorkflowTransition transition = workflowDefinitionStructMapper.toTransitionEntity(transitionETO);
            WorkflowDefinitionNodeETO fromNode = nodeConfigMap.get(transitionETO.getFromNodeCode());
            WorkflowDefinitionNodeETO toNode = nodeConfigMap.get(transitionETO.getToNodeCode());
            transition.setDefinitionId(definitionId);
            transition.setFromNodeId(nodeIdMap.get(transitionETO.getFromNodeCode()));
            transition.setFromNodeName(fromNode.getName());
            transition.setFromNodeType(fromNode.getNodeType());
            transition.setToNodeId(nodeIdMap.get(transitionETO.getToNodeCode()));
            transition.setToNodeName(toNode.getName());
            transition.setToNodeType(toNode.getNodeType());
            transition.setIsDefault(Objects.nonNull(transitionETO.getIsDefault()) ? transitionETO.getIsDefault() : DEFAULT_BRANCH_NO);
            transition.setPriority(Objects.nonNull(transitionETO.getPriority()) ? transitionETO.getPriority() : 0);
            workflowTransitionMapper.insert(transition);
        }
        workflowNodeApproverDeptExpandService.rebuildByApproverIds(deptApproverIds);
    }

    /**
     * 校验条件节点和并行拆分节点的默认分支规则。
     */
    private void validateTransitionDefaultRule(Map<String, WorkflowDefinitionNodeETO> nodeMap,
                                               List<WorkflowTransitionETO> transitions) {
        Map<String, List<WorkflowTransitionETO>> transitionGroup = transitions.stream()
                .collect(Collectors.groupingBy(WorkflowTransitionETO::getFromNodeCode, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<WorkflowTransitionETO>> entry : transitionGroup.entrySet()) {
            WorkflowDefinitionNodeETO node = nodeMap.get(entry.getKey());
            if (Objects.isNull(node)) {
                continue;
            }
            if (!requiresDefaultBranch(node.getNodeType(), entry.getValue().size())) {
                continue;
            }
            long defaultCount = entry.getValue().stream()
                    .filter(item -> DEFAULT_BRANCH_YES.equals(normalizeDefaultFlag(item.getIsDefault())))
                    .count();
            if (defaultCount != 1) {
                throw new BizException(node.getName() + "必须且只能配置一条默认分支");
            }
        }
    }

    /**
     * 发布前按已落库数据再次校验默认分支规则。
     */
    private void validatePublishTransitionRules(Long definitionId) {
        List<WorkflowNode> nodeList = workflowNodeMapper.selectList(new LambdaQueryWrapper<WorkflowNode>()
                .eq(WorkflowNode::getDefinitionId, definitionId));
        List<WorkflowTransition> transitionList = workflowTransitionMapper.selectList(new LambdaQueryWrapper<WorkflowTransition>()
                .eq(WorkflowTransition::getDefinitionId, definitionId));
        if (CollectionUtils.isEmpty(nodeList) || CollectionUtils.isEmpty(transitionList)) {
            return;
        }
        Map<Long, WorkflowNode> nodeMap = nodeList.stream()
                .collect(Collectors.toMap(WorkflowNode::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<WorkflowTransition>> transitionGroup = transitionList.stream()
                .collect(Collectors.groupingBy(WorkflowTransition::getFromNodeId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<WorkflowTransition>> entry : transitionGroup.entrySet()) {
            WorkflowNode node = nodeMap.get(entry.getKey());
            if (Objects.isNull(node)) {
                continue;
            }
            if (!requiresDefaultBranch(node.getNodeType(), entry.getValue().size())) {
                continue;
            }
            long defaultCount = entry.getValue().stream()
                    .filter(item -> DEFAULT_BRANCH_YES.equals(normalizeDefaultFlag(item.getIsDefault())))
                    .count();
            if (defaultCount != 1) {
                throw new BizException(node.getName() + "必须且只能配置一条默认分支");
            }
        }
    }

    /**
     * 判断当前节点是否需要强制校验默认分支。
     */
    private boolean requiresDefaultBranch(String nodeType, int outDegree) {
        if (outDegree <= 1) {
            return false;
        }
        return WorkflowNodeTypeEnum.CONDITION.getCode().equals(nodeType)
                || WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode().equals(nodeType);
    }

    /**
     * 规范化默认分支标记。
     */
    private Integer normalizeDefaultFlag(Integer isDefault) {
        return DEFAULT_BRANCH_YES.equals(isDefault) ? DEFAULT_BRANCH_YES : DEFAULT_BRANCH_NO;
    }

    /**
     * 删除定义下的子表数据。
     */
    private void deleteChildren(List<Long> definitionIds) {
        List<WorkflowNode> nodeList = workflowNodeMapper.selectList(new LambdaQueryWrapper<WorkflowNode>()
                .in(WorkflowNode::getDefinitionId, definitionIds));
        if (!CollectionUtils.isEmpty(nodeList)) {
            List<Long> nodeIds = nodeList.stream().map(WorkflowNode::getId).toList();
            List<WorkflowNodeApprover> approverList = workflowNodeApproverService.listByNodeIds(nodeIds);
            if (!CollectionUtils.isEmpty(approverList)) {
                workflowNodeApproverDeptExpandService.removeByApproverIds(approverList.stream().map(WorkflowNodeApprover::getId).toList());
                workflowNodeApproverService.removeByNodeIds(nodeIds);
            }
            workflowNodeMapper.removeByIds(nodeIds);
        }

        List<WorkflowTransition> transitionList = workflowTransitionMapper.selectList(new LambdaQueryWrapper<WorkflowTransition>()
                .in(WorkflowTransition::getDefinitionId, definitionIds));
        if (!CollectionUtils.isEmpty(transitionList)) {
            workflowTransitionMapper.removeByIds(transitionList.stream().map(WorkflowTransition::getId).toList());
        }
    }

    /**
     * 组装节点到审批人列表的映射关系。
     */
    private Map<Long, List<WorkflowNodeApproverVO>> buildApproverMap(Set<Long> nodeIds) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyMap();
        }
        List<WorkflowNodeApprover> approverList = workflowNodeApproverService.listByNodeIds(new ArrayList<>(nodeIds));
        if (CollectionUtils.isEmpty(approverList)) {
            return Collections.emptyMap();
        }
        Map<Long, List<WorkflowNodeApproverVO>> result = new HashMap<>();
        for (WorkflowNodeApprover approver : approverList) {
            WorkflowNodeApproverVO vo = workflowDefinitionStructMapper.toApproverVO(approver);
            vo.setApproverTypeMsg(WorkflowApproverTypeEnum.getMsgByCode(approver.getApproverType()));
            result.computeIfAbsent(approver.getNodeId(), key -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    /**
     * 规范化删除参数。
     */
    private List<Long> normalizeDeleteIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            throw new BizException("idList不能为空");
        }
        List<Long> result = idList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(result)) {
            throw new BizException("idList不能为空");
        }
        return result;
    }

    /**
     * 将组织审批人值解析为单个组织ID。
     */
    private Long parseApproverDeptId(String approverValue) {
        if (!StringUtils.hasText(approverValue) || approverValue.contains(",")) {
            return null;
        }
        try {
            return Long.valueOf(approverValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 构造节点审批人集合。
     */
    private List<WorkflowNodeApproverETO> buildApproverList(String approverType, Object approverIdsValue) {
        List<String> approverIdList = toStringList(approverIdsValue);
        if (CollectionUtils.isEmpty(approverIdList)) {
            return Collections.emptyList();
        }
        List<WorkflowNodeApproverETO> approverList = new ArrayList<>();
        for (int i = 0; i < approverIdList.size(); i++) {
            WorkflowNodeApproverETO approver = new WorkflowNodeApproverETO();
            approver.setApproverType(trimToNull(approverType));
            approver.setApproverValue(approverIdList.get(i));
            approver.setSortOrder(i + 1);
            approverList.add(approver);
        }
        return approverList;
    }

    /**
     * 解析节点名称。
     */
    private String resolveNodeName(WorkflowGraphNode rawNode) {
        if (Objects.nonNull(rawNode.text) && StringUtils.hasText(rawNode.text.value)) {
            return rawNode.text.value.trim();
        }
        return trimToNull(rawNode.id);
    }

    /**
     * 将前端节点角色映射为后端节点类型。
     */
    private String resolveNodeType(String clientNodeId,
                                   String nodeName,
                                   String nodeRole,
                                   int inDegree,
                                   int outDegree) {
        String normalizedNodeRole = trimToNull(nodeRole);
        if (WorkflowNodeTypeEnum.containsCode(normalizedNodeRole)) {
            return normalizedNodeRole;
        }
        if (FRONT_NODE_ROLE_START_END.equals(normalizedNodeRole)) {
            if (inDegree == 0 && outDegree > 0) {
                return WorkflowNodeTypeEnum.START.getCode();
            }
            if (outDegree == 0 && inDegree > 0) {
                return WorkflowNodeTypeEnum.END.getCode();
            }
            if ("START".equalsIgnoreCase(clientNodeId) || "开始".equals(nodeName)) {
                return WorkflowNodeTypeEnum.START.getCode();
            }
            if ("END".equalsIgnoreCase(clientNodeId) || "结束".equals(nodeName)) {
                return WorkflowNodeTypeEnum.END.getCode();
            }
        }
        throw new BizException("nodeRole不合法");
    }

    /**
     * 将前端审批模式映射为后端审批模式。
     */
    private String resolveApproveMode(String approveMode) {
        String normalizedApproveMode = trimToNull(approveMode);
        if (!StringUtils.hasText(normalizedApproveMode)) {
            return null;
        }
        return switch (normalizedApproveMode) {
            case FRONT_APPROVE_MODE_COUNTERSIGN -> WorkflowApproveModeEnum.AND.getCode();
            case FRONT_APPROVE_MODE_OR_SIGN -> WorkflowApproveModeEnum.OR.getCode();
            default -> normalizedApproveMode;
        };
    }

    /**
     * 将前端超时策略映射为后端超时策略。
     */
    private String resolveTimeoutAction(String timeoutStrategy) {
        String normalizedTimeoutStrategy = trimToNull(timeoutStrategy);
        if (!StringUtils.hasText(normalizedTimeoutStrategy)) {
            return null;
        }
        return switch (normalizedTimeoutStrategy) {
            case FRONT_TIMEOUT_ACTION_REMIND_ONLY -> WorkflowTimeoutActionEnum.NOTIFY_ONLY.getCode();
            case FRONT_TIMEOUT_ACTION_AUTO_PASS -> WorkflowTimeoutActionEnum.AUTO_APPROVE.getCode();
            default -> normalizedTimeoutStrategy;
        };
    }

    /**
     * 解析连线标签。
     */
    private String resolveEdgeLabel(WorkflowGraphEdge rawEdge) {
        if (Objects.nonNull(rawEdge.text) && StringUtils.hasText(rawEdge.text.value)) {
            return rawEdge.text.value.trim();
        }
        return null;
    }

    /**
     * 将输入值转为整数。
     */
    private Integer getInteger(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value instanceof String stringValue) {
            String trimmed = trimToNull(stringValue);
            if (!StringUtils.hasText(trimmed)) {
                return null;
            }
            try {
                return Integer.valueOf(trimmed);
            } catch (NumberFormatException ex) {
                throw new BizException("流程JSON中的数值字段格式不正确");
            }
        }
        throw new BizException("流程JSON中的数值字段格式不正确");
    }

    /**
     * 将输入值转为是否默认分支标记。
     */
    private Integer getYesNoFlag(Object value) {
        if (Objects.isNull(value)) {
            return DEFAULT_BRANCH_NO;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? DEFAULT_BRANCH_YES : DEFAULT_BRANCH_NO;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() == 1 ? DEFAULT_BRANCH_YES : DEFAULT_BRANCH_NO;
        }
        if (value instanceof String stringValue) {
            String trimmed = trimToNull(stringValue);
            if (!StringUtils.hasText(trimmed)) {
                return DEFAULT_BRANCH_NO;
            }
            if ("true".equalsIgnoreCase(trimmed) || "1".equals(trimmed)) {
                return DEFAULT_BRANCH_YES;
            }
            if ("false".equalsIgnoreCase(trimmed) || "0".equals(trimmed)) {
                return DEFAULT_BRANCH_NO;
            }
        }
        throw new BizException("流程JSON中的默认分支字段格式不正确");
    }

    /**
     * 从属性中提取字符串值。
     */
    private String getString(Map<String, Object> properties, String key) {
        if (CollectionUtils.isEmpty(properties)) {
            return null;
        }
        Object value = properties.get(key);
        if (Objects.isNull(value)) {
            return null;
        }
        return String.valueOf(value);
    }

    /**
     * 将输入对象规范化为字符串列表。
     */
    private List<String> toStringList(Object value) {
        if (Objects.isNull(value)) {
            return Collections.emptyList();
        }
        if (value instanceof List<?> listValue) {
            return listValue.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(this::trimToNull)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        String singleValue = trimToNull(String.valueOf(value));
        return StringUtils.hasText(singleValue) ? List.of(singleValue) : Collections.emptyList();
    }

    /**
     * 统一去除空白字符串。
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 前端流程图解析结果。
     */
    private record ParsedWorkflow(List<WorkflowDefinitionNodeETO> nodes,
                                  List<WorkflowTransitionETO> transitions) {
    }

    /**
     * 前端流程图根对象。
     */
    private static class WorkflowGraphPayload {
        public List<WorkflowGraphNode> nodes;
        public List<WorkflowGraphEdge> edges;
    }

    /**
     * 前端流程图节点对象。
     */
    private static class WorkflowGraphNode {
        public String id;
        public Object x;
        public Object y;
        public Map<String, Object> properties;
        public WorkflowGraphText text;
    }

    /**
     * 前端流程图连线对象。
     */
    private static class WorkflowGraphEdge {
        public Map<String, Object> properties;
        public String sourceNodeId;
        public String targetNodeId;
        public WorkflowGraphText text;
    }

    /**
     * 前端流程图文本对象。
     */
    private static class WorkflowGraphText {
        public String value;
    }
}
