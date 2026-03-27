package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.mapper.WorkflowNodeApproverMapper;
import com.yuyu.workflow.service.WorkflowNodeApproverService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 节点审批人配置服务实现。
 */
@Service
public class WorkflowNodeApproverServiceImpl extends ServiceImpl<WorkflowNodeApproverMapper, WorkflowNodeApprover> implements WorkflowNodeApproverService {

    /**
     * 注入节点审批人配置服务依赖。
     */
    public WorkflowNodeApproverServiceImpl(WorkflowNodeApproverMapper workflowNodeApproverMapper) {
        this.baseMapper = workflowNodeApproverMapper;
    }

    @Override
    public List<WorkflowNodeApprover> listByNodeIds(List<Long> nodeIds) {
        List<Long> normalizedNodeIds = normalizeNodeIds(nodeIds);
        if (CollectionUtils.isEmpty(normalizedNodeIds)) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<WorkflowNodeApprover>()
                .in(WorkflowNodeApprover::getNodeId, normalizedNodeIds)
                .orderByAsc(WorkflowNodeApprover::getNodeId, WorkflowNodeApprover::getSortOrder, WorkflowNodeApprover::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByNodeIds(List<Long> nodeIds) {
        List<WorkflowNodeApprover> approverList = listByNodeIds(nodeIds);
        if (CollectionUtils.isEmpty(approverList)) {
            return;
        }
        baseMapper.removeByIds(approverList.stream()
                .map(WorkflowNodeApprover::getId)
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveApprovers(List<WorkflowNodeApprover> approverList) {
        if (CollectionUtils.isEmpty(approverList)) {
            return;
        }
        for (WorkflowNodeApprover approver : approverList) {
            save(approver);
        }
    }

    /**
     * 规范化节点主键集合。
     */
    private List<Long> normalizeNodeIds(List<Long> nodeIds) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyList();
        }
        return nodeIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
