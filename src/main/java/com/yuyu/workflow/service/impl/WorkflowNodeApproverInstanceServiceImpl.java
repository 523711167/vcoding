package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.mapper.WorkflowNodeApproverInstanceMapper;
import com.yuyu.workflow.service.WorkflowNodeApproverInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 节点审批人实例服务实现。
 */
@Service
public class WorkflowNodeApproverInstanceServiceImpl implements WorkflowNodeApproverInstanceService {

    private final WorkflowNodeApproverInstanceMapper workflowNodeApproverInstanceMapper;

    /**
     * 注入节点审批人实例服务依赖。
     */
    public WorkflowNodeApproverInstanceServiceImpl(WorkflowNodeApproverInstanceMapper workflowNodeApproverInstanceMapper) {
        this.workflowNodeApproverInstanceMapper = workflowNodeApproverInstanceMapper;
    }

    @Override
    public List<WorkflowNodeApproverInstance> listByInstanceIds(List<Long> instanceIdList) {
        List<Long> normalizedIds = normalizeIds(instanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return workflowNodeApproverInstanceMapper.selectList(new LambdaQueryWrapper<WorkflowNodeApproverInstance>()
                .in(WorkflowNodeApproverInstance::getInstanceId, normalizedIds)
                .orderByAsc(WorkflowNodeApproverInstance::getId));
    }

    @Override
    public List<WorkflowNodeApproverInstance> listByNodeInstanceIds(List<Long> nodeInstanceIdList) {
        List<Long> normalizedIds = normalizeIds(nodeInstanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return workflowNodeApproverInstanceMapper.selectList(new LambdaQueryWrapper<WorkflowNodeApproverInstance>()
                .in(WorkflowNodeApproverInstance::getNodeInstanceId, normalizedIds)
                .orderByAsc(WorkflowNodeApproverInstance::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByInstanceIds(List<Long> instanceIdList) {
        List<WorkflowNodeApproverInstance> approverInstances = listByInstanceIds(instanceIdList);
        if (CollectionUtils.isEmpty(approverInstances)) {
            return;
        }
        workflowNodeApproverInstanceMapper.removeByIds(approverInstances.stream()
                .map(WorkflowNodeApproverInstance::getId)
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByNodeInstanceIds(List<Long> nodeInstanceIdList) {
        List<WorkflowNodeApproverInstance> approverInstances = listByNodeInstanceIds(nodeInstanceIdList);
        if (CollectionUtils.isEmpty(approverInstances)) {
            return;
        }
        workflowNodeApproverInstanceMapper.removeByIds(approverInstances.stream()
                .map(WorkflowNodeApproverInstance::getId)
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
