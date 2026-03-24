package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.mapper.WorkflowApprovalRecordMapper;
import com.yuyu.workflow.service.WorkflowApprovalRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 审批操作记录服务实现。
 */
@Service
public class WorkflowApprovalRecordServiceImpl implements WorkflowApprovalRecordService {

    private final WorkflowApprovalRecordMapper workflowApprovalRecordMapper;

    /**
     * 注入审批操作记录服务依赖。
     */
    public WorkflowApprovalRecordServiceImpl(WorkflowApprovalRecordMapper workflowApprovalRecordMapper) {
        this.workflowApprovalRecordMapper = workflowApprovalRecordMapper;
    }

    @Override
    public List<WorkflowApprovalRecord> listByInstanceIds(List<Long> instanceIdList) {
        List<Long> normalizedIds = normalizeIds(instanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return workflowApprovalRecordMapper.selectList(new LambdaQueryWrapper<WorkflowApprovalRecord>()
                .in(WorkflowApprovalRecord::getInstanceId, normalizedIds)
                .orderByAsc(WorkflowApprovalRecord::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByInstanceIds(List<Long> instanceIdList) {
        List<WorkflowApprovalRecord> recordList = listByInstanceIds(instanceIdList);
        if (CollectionUtils.isEmpty(recordList)) {
            return;
        }
        workflowApprovalRecordMapper.removeByIds(recordList.stream()
                .map(WorkflowApprovalRecord::getId)
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
