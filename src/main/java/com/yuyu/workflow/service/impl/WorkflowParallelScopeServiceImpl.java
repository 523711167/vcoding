package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.enums.WorkflowNodeInstanceStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowParallelScope;
import com.yuyu.workflow.mapper.WorkflowParallelScopeMapper;
import com.yuyu.workflow.service.WorkflowParallelScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 流程并行作用域服务实现。
 */
@Service
public class WorkflowParallelScopeServiceImpl extends ServiceImpl<WorkflowParallelScopeMapper, WorkflowParallelScope> implements WorkflowParallelScopeService {

    /**
     * 注入流程并行作用域服务依赖。
     */
    public WorkflowParallelScopeServiceImpl(WorkflowParallelScopeMapper workflowParallelScopeMapper) {
        this.baseMapper = workflowParallelScopeMapper;
    }

    @Override
    public List<WorkflowParallelScope> listByInstanceIds(List<Long> instanceIdList) {
        List<Long> normalizedIds = normalizeIds(instanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<WorkflowParallelScope>()
                .in(WorkflowParallelScope::getInstanceId, normalizedIds)
                .orderByAsc(WorkflowParallelScope::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByInstanceIds(List<Long> instanceIdList) {
        List<WorkflowParallelScope> scopeList = listByInstanceIds(instanceIdList);
        if (CollectionUtils.isEmpty(scopeList)) {
            return;
        }
        baseMapper.removeByIds(scopeList.stream()
                .map(WorkflowParallelScope::getId)
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowParallelScope createOnParallelSplitEnter(WorkflowInstance workflowInstance,
                                                            WorkflowNode splitDefinitionNode,
                                                            WorkflowNode joinDefinitionNode,
                                                            Long parentScopeId,
                                                            Integer expectedBranchCount) {
        if (Objects.isNull(workflowInstance)) {
            throw new BizException("流程实例不能为空");
        }
        if (Objects.isNull(splitDefinitionNode)) {
            throw new BizException("并行拆分节点不能为空");
        }
        if (Objects.isNull(joinDefinitionNode)) {
            throw new BizException("并行聚合节点不能为空");
        }

        WorkflowParallelScope workflowParallelScope = new WorkflowParallelScope();
        workflowParallelScope.setInstanceId(workflowInstance.getId());
        workflowParallelScope.setDefinitionId(workflowInstance.getDefinitionId());
        workflowParallelScope.setSplitDefinitionNodeId(splitDefinitionNode.getId());
        workflowParallelScope.setSplitDefinitionNodeName(splitDefinitionNode.getName());
        workflowParallelScope.setSplitDefinitionNodeType(splitDefinitionNode.getNodeType());
        workflowParallelScope.setJoinDefinitionNodeId(joinDefinitionNode.getId());
        workflowParallelScope.setJoinDefinitionNodeName(joinDefinitionNode.getName());
        workflowParallelScope.setJoinDefinitionNodeType(joinDefinitionNode.getNodeType());
        workflowParallelScope.setParentScopeId(parentScopeId);
        workflowParallelScope.setStatus(WorkflowNodeInstanceStatusEnum.ACTIVE.getCode());
        workflowParallelScope.setExpectedBranchCount(Objects.requireNonNullElse(expectedBranchCount, 0));
        workflowParallelScope.setArrivedBranchCount(0);
        save(workflowParallelScope);
        return workflowParallelScope;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WorkflowParallelScope workflowParallelScope) {
        if (Objects.isNull(workflowParallelScope)) {
            throw new BizException("并行作用域不能为空");
        }
        if (baseMapper.insert(workflowParallelScope) != 1) {
            throw new BizException("并行作用域保存失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WorkflowParallelScope workflowParallelScope) {
        if (Objects.isNull(workflowParallelScope) || Objects.isNull(workflowParallelScope.getId())) {
            throw new BizException("并行作用域id不能为空");
        }
        if (!super.updateById(workflowParallelScope)) {
            throw new BizException("并行作用域更新失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> idList) {
        List<Long> normalizedIds = normalizeIds(idList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return true;
        }
        return baseMapper.removeByIds(normalizedIds) > 0;
    }

    /**
     * 分支到达并行聚合节点后，累计当前并行作用域已到达分支数。
     *
     * @param parallelScopeId
     */
    @Override
    public void markParallelBranchArrived(Long parallelScopeId) {
        WorkflowParallelScope workflowParallelScope = getById(parallelScopeId);
        if (Objects.isNull(workflowParallelScope)) {
            throw new BizException("并行作用域不存在");
        }

        WorkflowParallelScope scopeUpdate = new WorkflowParallelScope();
        scopeUpdate.setId(parallelScopeId);
        scopeUpdate.setArrivedBranchCount(workflowParallelScope.getArrivedBranchCount() + 1);
        super.updateById(scopeUpdate);
    }

    /**
     * 规范化主键集合。
     */
    private List<Long> normalizeIds(Collection<?> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }
        return idList.stream()
                .peek(id -> Assert.isInstanceOf(Long.class, id, "主键类型必须为Long"))
                .map(Long.class::cast)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }


}
