package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.WorkflowApprovalActionEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowAuditETO;
import com.yuyu.workflow.mapper.WorkflowApprovalRecordMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import com.yuyu.workflow.service.WorkflowApprovalRecordService;
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
 * 审批操作记录服务实现。
 */
@Service
public class WorkflowApprovalRecordServiceImpl extends ServiceImpl<WorkflowApprovalRecordMapper, WorkflowApprovalRecord> implements WorkflowApprovalRecordService {

    private final WorkflowNodeMapper workflowNodeMapper;

    /**
     * 注入审批操作记录服务依赖。
     */
    public WorkflowApprovalRecordServiceImpl(WorkflowApprovalRecordMapper workflowApprovalRecordMapper,
                                             WorkflowNodeMapper workflowNodeMapper) {
        this.baseMapper = workflowApprovalRecordMapper;
        this.workflowNodeMapper = workflowNodeMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WorkflowApprovalRecord workflowApprovalRecord) {
        if (Objects.isNull(workflowApprovalRecord)) {
            throw new BizException("审批操作记录不能为空");
        }
        if (baseMapper.insert(workflowApprovalRecord) != 1) {
            throw new BizException("审批操作记录保存失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WorkflowApprovalRecord> workflowApprovalRecordList) {
        if (CollectionUtils.isEmpty(workflowApprovalRecordList)) {
            return true;
        }
        for (WorkflowApprovalRecord workflowApprovalRecord : workflowApprovalRecordList) {
            save(workflowApprovalRecord);
        }
        return true;
    }

    @Override
    public List<WorkflowApprovalRecord> listByInstanceIds(List<Long> instanceIdList) {
        List<Long> normalizedIds = normalizeIds(instanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<WorkflowApprovalRecord>()
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
        baseMapper.removeByIds(recordList.stream()
                .map(WorkflowApprovalRecord::getId)
                .toList());
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

    @Override
    public void insertRecordForReject(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance, WorkflowNodeInstance toWorkflowNodeInstance) {
        WorkflowApprovalRecord record = buildApprovalRecord(eto, workflowNodeInstance, toWorkflowNodeInstance, WorkflowApprovalActionEnum.REJECT);
        baseMapper.insert(record);
    }

    @Override
    public void insertRecordForApprove(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance, WorkflowNodeInstance toWorkflowNodeInstance) {
        WorkflowApprovalRecord record = buildApprovalRecord(eto, workflowNodeInstance, toWorkflowNodeInstance, WorkflowApprovalActionEnum.APPROVE);
        baseMapper.insert(record);
    }

    @Override
    public void insertRecordForRoute(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance, WorkflowNodeInstance toWorkflowNodeInstance) {
        WorkflowApprovalRecord record = buildApprovalRecord(eto, workflowNodeInstance, toWorkflowNodeInstance, WorkflowApprovalActionEnum.ROUTE);
        baseMapper.insert(record);
    }

    private static WorkflowApprovalRecord buildApprovalRecord(WorkflowAuditETO eto, WorkflowNodeInstance workflowNodeInstance,
                                                              WorkflowNodeInstance toWorkflowNodeInstance, WorkflowApprovalActionEnum workflowApprovalActionEnum) {
        WorkflowApprovalRecord record = new WorkflowApprovalRecord();
        record.setInstanceId(eto.getInstanceId());
        record.setNodeInstanceId(eto.getNodeInstanceId());
        record.setOperatorId(eto.getCurrentUserId());
        record.setOperatorName(eto.getCurrentUsername());
        record.setAction(workflowApprovalActionEnum.getCode());
        record.setNodeInstanceType(workflowNodeInstance.getDefinitionNodeType());
        record.setNodeInstanceName(workflowNodeInstance.getDefinitionNodeName());
        record.setComment(eto.getComment());
        record.setFromNodeId(workflowNodeInstance.getId());
        record.setFromNodeType(workflowNodeInstance.getDefinitionNodeType());
        record.setFromNodeName(workflowNodeInstance.getDefinitionNodeName());
        record.setToNodeId(toWorkflowNodeInstance.getId());
        record.setToNodeType(toWorkflowNodeInstance.getDefinitionNodeType());
        record.setToNodeName(toWorkflowNodeInstance.getDefinitionNodeName());
        record.setExtraData(null);
        record.setOperatedAt(OperationTimeContext.get());
        return record;
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
