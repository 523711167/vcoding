package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.WorkflowNodeApproverInstanceStatusEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.eto.workflow.WorkflowRejectAuditETO;
import com.yuyu.workflow.mapper.WorkflowNodeApproverInstanceMapper;
import com.yuyu.workflow.service.WorkflowNodeApproverInstanceService;
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
 * 节点审批人实例服务实现。
 */
@Service
public class WorkflowNodeApproverInstanceServiceImpl extends ServiceImpl<WorkflowNodeApproverInstanceMapper, WorkflowNodeApproverInstance> implements WorkflowNodeApproverInstanceService {

    /**
     * 注入节点审批人实例服务依赖。
     */
    public WorkflowNodeApproverInstanceServiceImpl(WorkflowNodeApproverInstanceMapper workflowNodeApproverInstanceMapper) {
        this.baseMapper = workflowNodeApproverInstanceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WorkflowNodeApproverInstance workflowNodeApproverInstance) {
        if (Objects.isNull(workflowNodeApproverInstance)) {
            throw new BizException("节点审批人实例不能为空");
        }
        if (baseMapper.insert(workflowNodeApproverInstance) != 1) {
            throw new BizException("节点审批人实例保存失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WorkflowNodeApproverInstance> workflowNodeApproverInstanceList) {
        if (CollectionUtils.isEmpty(workflowNodeApproverInstanceList)) {
            return true;
        }
        for (WorkflowNodeApproverInstance workflowNodeApproverInstance : workflowNodeApproverInstanceList) {
            save(workflowNodeApproverInstance);
        }
        return true;
    }

    @Override
    public List<WorkflowNodeApproverInstance> listByInstanceIds(List<Long> instanceIdList) {
        List<Long> normalizedIds = normalizeIds(instanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<WorkflowNodeApproverInstance>()
                .in(WorkflowNodeApproverInstance::getInstanceId, normalizedIds)
                .orderByAsc(WorkflowNodeApproverInstance::getId));
    }

    @Override
    public List<WorkflowNodeApproverInstance> listByNodeInstanceIds(List<Long> nodeInstanceIdList) {
        List<Long> normalizedIds = normalizeIds(nodeInstanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<WorkflowNodeApproverInstance>()
                .in(WorkflowNodeApproverInstance::getNodeInstanceId, normalizedIds)
                .orderByAsc(WorkflowNodeApproverInstance::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WorkflowNodeApproverInstance workflowNodeApproverInstance) {
        if (Objects.isNull(workflowNodeApproverInstance) || Objects.isNull(workflowNodeApproverInstance.getId())) {
            throw new BizException("节点审批人实例id不能为空");
        }
        if (!super.updateById(workflowNodeApproverInstance)) {
            throw new BizException("节点审批人实例更新失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByInstanceIds(List<Long> instanceIdList) {
        List<WorkflowNodeApproverInstance> approverInstances = listByInstanceIds(instanceIdList);
        if (CollectionUtils.isEmpty(approverInstances)) {
            return;
        }
        baseMapper.removeByIds(approverInstances.stream()
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
        baseMapper.removeByIds(approverInstances.stream()
                .map(WorkflowNodeApproverInstance::getId)
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
    public void reject(WorkflowRejectAuditETO workflowRejectAuditETO) {

    }

    @Override
    public void updateNodeApproverForReject(Long nodeInstanceId, String comment, Long approverInstanceId) {
        WorkflowNodeApproverInstance update = new WorkflowNodeApproverInstance();
        update.setStatus(WorkflowNodeApproverInstanceStatusEnum.REJECTED.getCode());
        update.setFinishedAt(OperationTimeContext.get());
        update.setComment(comment);
        baseMapper.update(update, new LambdaQueryWrapper<WorkflowNodeApproverInstance>()
                .eq(Objects.nonNull(approverInstanceId), WorkflowNodeApproverInstance::getId, approverInstanceId)
                .eq(WorkflowNodeApproverInstance::getNodeInstanceId, nodeInstanceId)
                .eq(WorkflowNodeApproverInstance::getIsActive, YesNoEnum.YES.getId())
                .in(WorkflowNodeApproverInstance::getStatus,
                        List.of(WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode(),
                                WorkflowNodeApproverInstanceStatusEnum.WAITING_ADD_SIGN.getCode())
                ));

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
