package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.mapper.WorkflowNodeInstanceMapper;
import com.yuyu.workflow.service.WorkflowNodeInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 节点实例服务实现。
 */
@Service
public class WorkflowNodeInstanceServiceImpl implements WorkflowNodeInstanceService {

    private final WorkflowNodeInstanceMapper workflowNodeInstanceMapper;

    /**
     * 注入节点实例服务依赖。
     */
    public WorkflowNodeInstanceServiceImpl(WorkflowNodeInstanceMapper workflowNodeInstanceMapper) {
        this.workflowNodeInstanceMapper = workflowNodeInstanceMapper;
    }

    @Override
    public List<WorkflowNodeInstance> listByInstanceIds(List<Long> instanceIdList) {
        List<Long> normalizedIds = normalizeIds(instanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return workflowNodeInstanceMapper.selectList(new LambdaQueryWrapper<WorkflowNodeInstance>()
                .in(WorkflowNodeInstance::getInstanceId, normalizedIds)
                .orderByAsc(WorkflowNodeInstance::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByInstanceIds(List<Long> instanceIdList) {
        List<WorkflowNodeInstance> nodeInstanceList = listByInstanceIds(instanceIdList);
        if (CollectionUtils.isEmpty(nodeInstanceList)) {
            return;
        }
        workflowNodeInstanceMapper.removeByIds(nodeInstanceList.stream()
                .map(WorkflowNodeInstance::getId)
                .toList());
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
