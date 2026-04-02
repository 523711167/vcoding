package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.mapper.WorkflowTransitionMapper;
import com.yuyu.workflow.service.WorkflowTransitionService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 流程连线服务实现。
 */
@Service
public class WorkflowTransitionServiceImpl extends ServiceImpl<WorkflowTransitionMapper, WorkflowTransition> implements WorkflowTransitionService {

    /**
     * 注入流程连线服务依赖。
     */
    public WorkflowTransitionServiceImpl(WorkflowTransitionMapper workflowTransitionMapper) {
        this.baseMapper = workflowTransitionMapper;
    }

    @Override
    public WorkflowTransition getByIdOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        WorkflowTransition workflowTransition = getById(id);
        if (Objects.isNull(workflowTransition)) {
            throw new BizException("流程连线不存在");
        }
        return workflowTransition;
    }

    @Override
    public List<WorkflowTransition> listByDefinitionId(Long definitionId) {
        if (Objects.isNull(definitionId)) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<WorkflowTransition>()
                .eq(WorkflowTransition::getDefinitionId, definitionId)
                .orderByAsc(WorkflowTransition::getPriority, WorkflowTransition::getId));
    }

    @Override
    public List<WorkflowTransition> listByFromNodeId(Long fromNodeId) {
        if (Objects.isNull(fromNodeId)) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<WorkflowTransition>()
                .eq(WorkflowTransition::getFromNodeId, fromNodeId)
                .orderByAsc(WorkflowTransition::getPriority, WorkflowTransition::getId));
    }

    @Override
    public void removeByDefinitionIds(List<Long> definitionIds) {
        List<Long> normalizedIds = normalizeIds(definitionIds);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return;
        }
        List<WorkflowTransition> transitionList = list(new LambdaQueryWrapper<WorkflowTransition>()
                .in(WorkflowTransition::getDefinitionId, normalizedIds)
                .select(WorkflowTransition::getId));
        if (CollectionUtils.isEmpty(transitionList)) {
            return;
        }
        baseMapper.removeByIds(transitionList.stream().map(WorkflowTransition::getId).toList());
    }

    /**
     * 规范化主键集合。
     */
    private List<Long> normalizeIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }
        return idList.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
