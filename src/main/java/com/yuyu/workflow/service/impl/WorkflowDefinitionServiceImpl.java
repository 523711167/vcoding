package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.enums.WorkflowApproveModeEnum;
import com.yuyu.workflow.common.enums.WorkflowApproverTypeEnum;
import com.yuyu.workflow.common.enums.WorkflowDefinitionStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.common.enums.WorkflowTimeoutActionEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.WorkflowDefinitionStructMapper;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionCreateETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionDisableETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionNodeETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionPublishETO;
import com.yuyu.workflow.eto.workflow.WorkflowDefinitionUpdateETO;
import com.yuyu.workflow.eto.workflow.WorkflowNodeApproverETO;
import com.yuyu.workflow.eto.workflow.WorkflowTransitionETO;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.mapper.WorkflowDefinitionMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import com.yuyu.workflow.mapper.WorkflowTransitionMapper;
import com.yuyu.workflow.qto.workflow.WorkflowDefinitionListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowDefinitionPageQTO;
import com.yuyu.workflow.service.WorkflowDefinitionService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现。
 */
@Service
public class WorkflowDefinitionServiceImpl implements WorkflowDefinitionService {

    private final WorkflowDefinitionMapper workflowDefinitionMapper;
    private final WorkflowNodeMapper workflowNodeMapper;
    private final WorkflowNodeApproverMapper workflowNodeApproverMapper;
    private final WorkflowTransitionMapper workflowTransitionMapper;
    private final WorkflowDefinitionStructMapper workflowDefinitionStructMapper;

    /**
     * 注入流程定义模块依赖。
     */
    public WorkflowDefinitionServiceImpl(WorkflowDefinitionMapper workflowDefinitionMapper,
                                         WorkflowNodeMapper workflowNodeMapper,
                                         WorkflowNodeApproverMapper workflowNodeApproverMapper,
                                         WorkflowTransitionMapper workflowTransitionMapper,
                                         WorkflowDefinitionStructMapper workflowDefinitionStructMapper) {
        this.workflowDefinitionMapper = workflowDefinitionMapper;
        this.workflowNodeMapper = workflowNodeMapper;
        this.workflowNodeApproverMapper = workflowNodeApproverMapper;
        this.workflowTransitionMapper = workflowTransitionMapper;
        this.workflowDefinitionStructMapper = workflowDefinitionStructMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionVO create(WorkflowDefinitionCreateETO eto) {
        validateDefinitionPayload(eto.getNodes(), eto.getTransitions());
        assertNoDraftVersion(eto.getCode());

        WorkflowDefinition entity = workflowDefinitionStructMapper.toEntity(eto);
        entity.setVersion(resolveNextVersion(eto.getCode()));
        entity.setStatus(WorkflowDefinitionStatusEnum.DRAFT.getId());
        entity.setCreatedBy(requireCurrentUserId(eto.getCurrentUserId()));
        workflowDefinitionMapper.insert(entity);
        persistChildren(entity.getId(), eto.getNodes(), eto.getTransitions());
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionVO update(WorkflowDefinitionUpdateETO eto) {
        WorkflowDefinition oldEntity = getDraftDefinitionOrThrow(eto.getId());
        validateDefinitionPayload(eto.getNodes(), eto.getTransitions());

        WorkflowDefinition entity = workflowDefinitionStructMapper.toUpdatedEntity(eto, oldEntity);
        workflowDefinitionMapper.updateById(entity);
        deleteChildren(List.of(entity.getId()));
        persistChildren(entity.getId(), eto.getNodes(), eto.getTransitions());
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
                buildDefinitionQuery(qto.getName(), qto.getCode(), qto.getBizCode(), qto.getStatus())));
    }

    @Override
    public PageVo<WorkflowDefinitionVO> page(WorkflowDefinitionPageQTO qto) {
        IPage<WorkflowDefinition> page = workflowDefinitionMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(qto.getPageNum(), qto.getPageSize()),
                buildDefinitionQuery(qto.getName(), qto.getCode(), qto.getBizCode(), qto.getStatus())
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
        Map<Long, WorkflowNode> nodeMap = nodeList.stream()
                .collect(Collectors.toMap(WorkflowNode::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<WorkflowNodeApproverVO>> approverMap = buildApproverMap(nodeMap.keySet());

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
        List<WorkflowTransitionVO> transitionVOList = new ArrayList<>();
        for (WorkflowTransition transition : transitionList) {
            WorkflowTransitionVO transitionVO = workflowDefinitionStructMapper.toTransitionVO(transition);
            WorkflowNode fromNode = nodeMap.get(transition.getFromNodeId());
            WorkflowNode toNode = nodeMap.get(transition.getToNodeId());
            transitionVO.setFromNodeCode(Objects.nonNull(fromNode) ? fromNode.getCode() : null);
            transitionVO.setToNodeCode(Objects.nonNull(toNode) ? toNode.getCode() : null);
            transitionVOList.add(transitionVO);
        }
        vo.setTransitionList(transitionVOList);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(WorkflowDefinitionPublishETO eto) {
        WorkflowDefinition definition = getDraftDefinitionOrThrow(eto.getId());
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
                                                                        String bizCode,
                                                                        Integer status) {
        return new LambdaQueryWrapper<WorkflowDefinition>()
                .like(StringUtils.hasText(name), WorkflowDefinition::getName, name)
                .like(StringUtils.hasText(code), WorkflowDefinition::getCode, code)
                .like(StringUtils.hasText(bizCode), WorkflowDefinition::getBizCode, bizCode)
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
            if (nodeMap.putIfAbsent(node.getCode(), node) != null) {
                throw new BizException("节点编码不能重复");
            }
            if (WorkflowNodeTypeEnum.START.getCode().equals(node.getNodeType())) {
                startCount++;
            }
            if (WorkflowNodeTypeEnum.END.getCode().equals(node.getNodeType())) {
                endCount++;
            }
            validateNodePayload(node);
        }
        if (startCount != 1) {
            throw new BizException("必须且只能存在一个开始节点");
        }
        if (endCount < 1) {
            throw new BizException("至少需要一个结束节点");
        }
        Map<String, List<String>> adjacencyMap = new HashMap<>();
        for (WorkflowTransitionETO transition : transitions) {
            if (!nodeMap.containsKey(transition.getFromNodeCode()) || !nodeMap.containsKey(transition.getToNodeCode())) {
                throw new BizException("连线引用了不存在的节点");
            }
            if (Objects.equals(transition.getFromNodeCode(), transition.getToNodeCode())) {
                throw new BizException("连线不能指向自身");
            }
            adjacencyMap.computeIfAbsent(transition.getFromNodeCode(), key -> new ArrayList<>()).add(transition.getToNodeCode());
        }
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
        List<WorkflowNodeApproverETO> approverList = Objects.nonNull(node.getApproverList())
                ? node.getApproverList()
                : Collections.emptyList();
        if (WorkflowNodeTypeEnum.APPROVAL.getCode().equals(node.getNodeType())) {
            if (CollectionUtils.isEmpty(approverList)) {
                throw new BizException("审批节点必须配置审批人");
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
                approver.setNodeId(node.getId());
                approver.setSortOrder(Objects.nonNull(approverETO.getSortOrder()) ? approverETO.getSortOrder() : 0);
                workflowNodeApproverMapper.insert(approver);
            }
        }

        for (WorkflowTransitionETO transitionETO : transitions) {
            WorkflowTransition transition = workflowDefinitionStructMapper.toTransitionEntity(transitionETO);
            transition.setDefinitionId(definitionId);
            transition.setFromNodeId(nodeIdMap.get(transitionETO.getFromNodeCode()));
            transition.setToNodeId(nodeIdMap.get(transitionETO.getToNodeCode()));
            transition.setPriority(Objects.nonNull(transitionETO.getPriority()) ? transitionETO.getPriority() : 0);
            workflowTransitionMapper.insert(transition);
        }
    }

    /**
     * 删除定义下的子表数据。
     */
    private void deleteChildren(List<Long> definitionIds) {
        List<WorkflowNode> nodeList = workflowNodeMapper.selectList(new LambdaQueryWrapper<WorkflowNode>()
                .in(WorkflowNode::getDefinitionId, definitionIds));
        if (!CollectionUtils.isEmpty(nodeList)) {
            List<Long> nodeIds = nodeList.stream().map(WorkflowNode::getId).toList();
            List<WorkflowNodeApprover> approverList = workflowNodeApproverMapper.selectList(new LambdaQueryWrapper<WorkflowNodeApprover>()
                    .in(WorkflowNodeApprover::getNodeId, nodeIds));
            if (!CollectionUtils.isEmpty(approverList)) {
                workflowNodeApproverMapper.removeByIds(approverList.stream().map(WorkflowNodeApprover::getId).toList());
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
        List<WorkflowNodeApprover> approverList = workflowNodeApproverMapper.selectList(new LambdaQueryWrapper<WorkflowNodeApprover>()
                .in(WorkflowNodeApprover::getNodeId, nodeIds)
                .orderByAsc(WorkflowNodeApprover::getSortOrder, WorkflowNodeApprover::getId));
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
}
