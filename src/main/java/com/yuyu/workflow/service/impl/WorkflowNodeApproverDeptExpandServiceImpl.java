package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.DeptRelationTypeEnum;
import com.yuyu.workflow.common.enums.OrgTypeEnum;
import com.yuyu.workflow.common.enums.WorkflowApproverTypeEnum;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.entity.WorkflowNodeApproverDeptExpand;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverDeptExpandMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import com.yuyu.workflow.service.WorkflowNodeApproverDeptExpandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作流节点审批组织展开关系维护服务实现。
 */
@Service
public class WorkflowNodeApproverDeptExpandServiceImpl extends ServiceImpl<WorkflowNodeApproverDeptExpandMapper, WorkflowNodeApproverDeptExpand> implements WorkflowNodeApproverDeptExpandService {

    private final WorkflowNodeApproverMapper workflowNodeApproverMapper;
    private final WorkflowNodeApproverDeptExpandMapper workflowNodeApproverDeptExpandMapper;
    private final WorkflowNodeMapper workflowNodeMapper;
    private final UserDeptMapper userDeptMapper;

    public WorkflowNodeApproverDeptExpandServiceImpl(WorkflowNodeApproverMapper workflowNodeApproverMapper,
                                                     WorkflowNodeApproverDeptExpandMapper workflowNodeApproverDeptExpandMapper,
                                                     WorkflowNodeMapper workflowNodeMapper,
                                                     UserDeptMapper userDeptMapper) {
        this.baseMapper = workflowNodeApproverDeptExpandMapper;
        this.workflowNodeApproverMapper = workflowNodeApproverMapper;
        this.workflowNodeApproverDeptExpandMapper = workflowNodeApproverDeptExpandMapper;
        this.workflowNodeMapper = workflowNodeMapper;
        this.userDeptMapper = userDeptMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildByApproverIds(List<Long> approverIds) {
        List<Long> normalizedApproverIds = normalizeApproverIds(approverIds);
        if (CollectionUtils.isEmpty(normalizedApproverIds)) {
            return;
        }
        removeByApproverIds(normalizedApproverIds);
        List<WorkflowNodeApprover> sourceApprovers = workflowNodeApproverMapper.selectList(new LambdaQueryWrapper<WorkflowNodeApprover>()
                .in(WorkflowNodeApprover::getId, normalizedApproverIds)
                .eq(WorkflowNodeApprover::getApproverType, WorkflowApproverTypeEnum.DEPT.getCode())
                .orderByAsc(WorkflowNodeApprover::getNodeId, WorkflowNodeApprover::getId));
        if (CollectionUtils.isEmpty(sourceApprovers)) {
            return;
        }

        Map<Long, WorkflowNode> nodeMap = buildNodeMap(sourceApprovers);
        Map<Long, UserDept> sourceDeptMap = buildEnabledSourceDeptMap(sourceApprovers);
        if (nodeMap.isEmpty() || sourceDeptMap.isEmpty()) {
            return;
        }
        Map<Long, List<UserDept>> childrenMap = buildChildrenMap(sourceDeptMap.values());
        List<WorkflowNodeApproverDeptExpand> expandRelations = new ArrayList<>();
        for (WorkflowNodeApprover approver : sourceApprovers) {
            Long sourceDeptId = parseDeptId(approver.getApproverValue());
            if (Objects.isNull(sourceDeptId)) {
                continue;
            }
            WorkflowNode node = nodeMap.get(approver.getNodeId());
            UserDept sourceDept = sourceDeptMap.get(sourceDeptId);
            if (Objects.isNull(node) || Objects.isNull(sourceDept)) {
                continue;
            }
            appendExpandRelations(expandRelations, approver, node, sourceDept, sourceDept, childrenMap,
                    DeptRelationTypeEnum.SELF.getCode(), 0);
        }
        for (WorkflowNodeApproverDeptExpand relation : expandRelations) {
            workflowNodeApproverDeptExpandMapper.insert(relation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildByDeptPaths(List<String> deptPaths) {
        Set<Long> ancestorDeptIds = parseAncestorDeptIds(deptPaths);
        if (CollectionUtils.isEmpty(ancestorDeptIds)) {
            return;
        }
        List<Long> approverIds = workflowNodeApproverMapper.selectList(new LambdaQueryWrapper<WorkflowNodeApprover>()
                        .eq(WorkflowNodeApprover::getApproverType, WorkflowApproverTypeEnum.DEPT.getCode())
                        .in(WorkflowNodeApprover::getApproverValue, ancestorDeptIds))
                .stream()
                .map(WorkflowNodeApprover::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        rebuildByApproverIds(approverIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByApproverIds(List<Long> approverIds) {
        List<Long> normalizedApproverIds = normalizeApproverIds(approverIds);
        if (CollectionUtils.isEmpty(normalizedApproverIds)) {
            return;
        }
        List<WorkflowNodeApproverDeptExpand> existRelations = workflowNodeApproverDeptExpandMapper.selectList(
                new LambdaQueryWrapper<WorkflowNodeApproverDeptExpand>()
                        .in(WorkflowNodeApproverDeptExpand::getApproverId, normalizedApproverIds));
        if (CollectionUtils.isEmpty(existRelations)) {
            return;
        }
        workflowNodeApproverDeptExpandMapper.removeByIds(existRelations.stream()
                .map(WorkflowNodeApproverDeptExpand::getId)
                .toList());
    }

    /**
     * 构造节点映射，供展开结果补充 definitionId。
     */
    private Map<Long, WorkflowNode> buildNodeMap(List<WorkflowNodeApprover> sourceApprovers) {
        Set<Long> nodeIds = sourceApprovers.stream()
                .map(WorkflowNodeApprover::getNodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyMap();
        }
        return workflowNodeMapper.selectList(new LambdaQueryWrapper<WorkflowNode>()
                        .in(WorkflowNode::getId, nodeIds))
                .stream()
                .collect(Collectors.toMap(WorkflowNode::getId, node -> node, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 查询可作为展开源节点的有效组织。
     */
    private Map<Long, UserDept> buildEnabledSourceDeptMap(List<WorkflowNodeApprover> sourceApprovers) {
        Set<Long> sourceDeptIds = sourceApprovers.stream()
                .map(WorkflowNodeApprover::getApproverValue)
                .map(this::parseDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(sourceDeptIds)) {
            return Collections.emptyMap();
        }
        return userDeptMapper.selectList(new LambdaQueryWrapper<UserDept>()
                        .in(UserDept::getId, sourceDeptIds)
                        .eq(UserDept::getStatus, CommonStatusEnum.ENABLED.getId()))
                .stream()
                .collect(Collectors.toMap(UserDept::getId, dept -> dept, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 查询所有参与本次展开的有效节点，并按父节点归组。
     */
    private Map<Long, List<UserDept>> buildChildrenMap(Iterable<UserDept> sourceDeptList) {
        List<String> sourcePaths = new ArrayList<>();
        for (UserDept sourceDept : sourceDeptList) {
            if (StringUtils.hasText(sourceDept.getPath())) {
                sourcePaths.add(sourceDept.getPath());
            }
        }
        if (CollectionUtils.isEmpty(sourcePaths)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<UserDept> queryWrapper = new LambdaQueryWrapper<UserDept>()
                .eq(UserDept::getStatus, CommonStatusEnum.ENABLED.getId())
                .and(wrapper -> {
                    boolean first = true;
                    for (String path : sourcePaths.stream().distinct().toList()) {
                        if (!first) {
                            wrapper.or();
                        }
                        wrapper.likeRight(UserDept::getPath, path);
                        first = false;
                    }
                })
                .orderByAsc(UserDept::getLevel, UserDept::getSortOrder, UserDept::getId);
        return userDeptMapper.selectList(queryWrapper).stream()
                .collect(Collectors.groupingBy(UserDept::getParentId, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 递归写入来源节点自身及其全部有效子孙节点。
     */
    private void appendExpandRelations(List<WorkflowNodeApproverDeptExpand> result,
                                       WorkflowNodeApprover approver,
                                       WorkflowNode node,
                                       UserDept sourceRelationDept,
                                       UserDept currentDept,
                                       Map<Long, List<UserDept>> childrenMap,
                                       String relationType,
                                       int distance) {
        WorkflowNodeApproverDeptExpand expandRelation = new WorkflowNodeApproverDeptExpand();
        expandRelation.setApproverId(approver.getId());
        expandRelation.setNodeId(node.getId());
        expandRelation.setDefinitionId(node.getDefinitionId());
        expandRelation.setSourceDeptId(sourceRelationDept.getId());
        expandRelation.setSourceOrgType(sourceRelationDept.getOrgType());
        expandRelation.setSourcePostType(resolvePostType(sourceRelationDept));
        expandRelation.setDeptId(currentDept.getId());
        expandRelation.setOrgType(currentDept.getOrgType());
        expandRelation.setPostType(resolvePostType(currentDept));
        expandRelation.setRelationType(relationType);
        expandRelation.setDistance(distance);
        result.add(expandRelation);

        for (UserDept child : childrenMap.getOrDefault(currentDept.getId(), Collections.emptyList())) {
            appendExpandRelations(result, approver, node, sourceRelationDept, child, childrenMap,
                    DeptRelationTypeEnum.DESCENDANT.getCode(), distance + 1);
        }
    }

    /**
     * 仅岗位节点保留岗位类型。
     */
    private String resolvePostType(UserDept dept) {
        if (!OrgTypeEnum.POST.getCode().equals(dept.getOrgType())) {
            return null;
        }
        return dept.getPostType();
    }

    /**
     * 将路径集合解析为祖先组织ID字符串集合。
     */
    private Set<Long> parseAncestorDeptIds(List<String> deptPaths) {
        if (CollectionUtils.isEmpty(deptPaths)) {
            return Collections.emptySet();
        }
        return deptPaths.stream()
                .filter(StringUtils::hasText)
                .flatMap(path -> Arrays.stream(path.split("/")))
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 解析审批组织ID。
     */
    private Long parseDeptId(Long approverValue) {
        if (Objects.isNull(approverValue) || approverValue <= 0) {
            return null;
        }
        return approverValue;
    }

    /**
     * 规范化审批人配置主键集合，去空并去重。
     */
    private List<Long> normalizeApproverIds(List<Long> approverIds) {
        if (CollectionUtils.isEmpty(approverIds)) {
            return Collections.emptyList();
        }
        return approverIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
