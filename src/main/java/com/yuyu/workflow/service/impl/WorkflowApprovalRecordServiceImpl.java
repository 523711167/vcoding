package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.WorkflowApprovalActionEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.mapper.WorkflowApprovalRecordMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
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
    private final WorkflowNodeMapper workflowNodeMapper;

    /**
     * 注入审批操作记录服务依赖。
     */
    public WorkflowApprovalRecordServiceImpl(WorkflowApprovalRecordMapper workflowApprovalRecordMapper, WorkflowNodeMapper workflowNodeMapper) {
        this.workflowApprovalRecordMapper = workflowApprovalRecordMapper;
        this.workflowNodeMapper = workflowNodeMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(WorkflowApprovalRecord workflowApprovalRecord) {
        if (Objects.isNull(workflowApprovalRecord)) {
            throw new BizException("审批操作记录不能为空");
        }
        if (workflowApprovalRecordMapper.insert(workflowApprovalRecord) != 1) {
            throw new BizException("审批操作记录保存失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<WorkflowApprovalRecord> workflowApprovalRecordList) {
        if (CollectionUtils.isEmpty(workflowApprovalRecordList)) {
            return;
        }
        for (WorkflowApprovalRecord workflowApprovalRecord : workflowApprovalRecordList) {
            save(workflowApprovalRecord);
        }
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


    @Override
    public void insertRecordForReject(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance) {
        WorkflowApprovalRecord record = new WorkflowApprovalRecord();
        record.setInstanceId(eto.getInstanceId());
        record.setNodeInstanceId(eto.getNodeInstanceId());
        record.setOperatorId(eto.getCurrentUserId());
        record.setOperatorName(eto.getCurrentUsername());
        record.setAction(WorkflowApprovalActionEnum.REJECT.getCode());
        record.setNodeInstanceType(workflowNodeInstance.getDefinitionNodeType());
        record.setNodeInstanceName(workflowNodeInstance.getDefinitionNodeName());
        record.setComment(eto.getComment());
        record.setFromNodeId(workflowNodeInstance.getId());
        record.setFromNodeType(workflowNodeInstance.getDefinitionNodeType());
        record.setFromNodeName(workflowNodeInstance.getDefinitionNodeName());
        record.setToNodeId(null);
        record.setToNodeType(WorkflowNodeTypeEnum.END.getCode());
        record.setToNodeName(WorkflowNodeTypeEnum.END.getName());
        record.setExtraData(null);
        record.setOperatedAt(OperationTimeContext.get());
        workflowApprovalRecordMapper.insert(record);
    }

    @Override
    public boolean isPreviousNodeParallelSplit(Long instanceId, Long nodeInstanceId) {
        WorkflowApprovalRecord latestIncomingRecord = workflowApprovalRecordMapper.selectOne(
                new LambdaQueryWrapper<WorkflowApprovalRecord>()
                        .eq(WorkflowApprovalRecord::getInstanceId, instanceId)
                        .eq(WorkflowApprovalRecord::getToNodeId, nodeInstanceId)
                        .orderByDesc(WorkflowApprovalRecord::getOperatedAt, WorkflowApprovalRecord::getId)
                        .last("LIMIT 1"));
        if (Objects.isNull(latestIncomingRecord)) {
            return false;
        }
        return Objects.equals(WorkflowNodeTypeEnum.PARALLEL_SPLIT.getCode(), latestIncomingRecord.getFromNodeType());
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
