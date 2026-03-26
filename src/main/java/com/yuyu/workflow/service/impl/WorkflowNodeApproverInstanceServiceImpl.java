package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.WorkflowNodeApproverInstanceStatusEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeTypeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.eto.workflow.WorkflowRejectAuditETO;
import com.yuyu.workflow.mapper.WorkflowApprovalRecordMapper;
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
    private final WorkflowApprovalRecordMapper workflowApprovalRecordMapper;
    private final WorkflowNodeInstanceServiceImpl workflowNodeInstanceServiceImpl;
    private final WorkflowInstanceServiceImpl workflowInstanceServiceImpl;
    private final WorkflowApprovalRecordServiceImpl workflowApprovalRecordServiceImpl;

    /**
     * 注入节点审批人实例服务依赖。
     */
    public WorkflowNodeApproverInstanceServiceImpl(WorkflowNodeApproverInstanceMapper workflowNodeApproverInstanceMapper,
                                                   WorkflowApprovalRecordMapper workflowApprovalRecordMapper, WorkflowNodeInstanceServiceImpl workflowNodeInstanceServiceImpl, WorkflowInstanceServiceImpl workflowInstanceServiceImpl, WorkflowApprovalRecordServiceImpl workflowApprovalRecordServiceImpl) {
        this.workflowNodeApproverInstanceMapper = workflowNodeApproverInstanceMapper;
        this.workflowApprovalRecordMapper = workflowApprovalRecordMapper;
        this.workflowNodeInstanceServiceImpl = workflowNodeInstanceServiceImpl;
        this.workflowInstanceServiceImpl = workflowInstanceServiceImpl;
        this.workflowApprovalRecordServiceImpl = workflowApprovalRecordServiceImpl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(WorkflowNodeApproverInstance workflowNodeApproverInstance) {
        if (Objects.isNull(workflowNodeApproverInstance)) {
            throw new BizException("节点审批人实例不能为空");
        }
        if (workflowNodeApproverInstanceMapper.insert(workflowNodeApproverInstance) != 1) {
            throw new BizException("节点审批人实例保存失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<WorkflowNodeApproverInstance> workflowNodeApproverInstanceList) {
        if (CollectionUtils.isEmpty(workflowNodeApproverInstanceList)) {
            return;
        }
        for (WorkflowNodeApproverInstance workflowNodeApproverInstance : workflowNodeApproverInstanceList) {
            save(workflowNodeApproverInstance);
        }
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
    public void updateById(WorkflowNodeApproverInstance workflowNodeApproverInstance) {
        if (Objects.isNull(workflowNodeApproverInstance) || Objects.isNull(workflowNodeApproverInstance.getId())) {
            throw new BizException("节点审批人实例id不能为空");
        }
        if (workflowNodeApproverInstanceMapper.updateById(workflowNodeApproverInstance) != 1) {
            throw new BizException("节点审批人实例更新失败");
        }
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


    @Override
    public void reject(WorkflowRejectAuditETO workflowRejectAuditETO) {


    }

    @Override
    public void updateNodeApproverForReject(Long nodeInstanceId, String comment) {
        WorkflowNodeApproverInstance update = new WorkflowNodeApproverInstance();
        update.setStatus(WorkflowNodeApproverInstanceStatusEnum.REJECTED.getCode());
        update.setComment(comment);
        workflowNodeApproverInstanceMapper.update(update, new LambdaQueryWrapper<WorkflowNodeApproverInstance>()
                .eq(WorkflowNodeApproverInstance::getNodeInstanceId, nodeInstanceId)
                .eq(WorkflowNodeApproverInstance::getFinishedAt, OperationTimeContext.get())
                .in(WorkflowNodeApproverInstance::getStatus,
                        List.of(WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode(),
                                WorkflowNodeApproverInstanceStatusEnum.WAITING_ADD_SIGN.getCode())
                ));

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
