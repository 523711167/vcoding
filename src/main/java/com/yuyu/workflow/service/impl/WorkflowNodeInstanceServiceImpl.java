package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.WorkflowNodeInstanceStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
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
    @Transactional(rollbackFor = Exception.class)
    public void save(WorkflowNodeInstance workflowNodeInstance) {
        if (Objects.isNull(workflowNodeInstance)) {
            throw new BizException("节点实例不能为空");
        }
        if (workflowNodeInstanceMapper.insert(workflowNodeInstance) != 1) {
            throw new BizException("节点实例保存失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<WorkflowNodeInstance> workflowNodeInstanceList) {
        if (CollectionUtils.isEmpty(workflowNodeInstanceList)) {
            return;
        }
        for (WorkflowNodeInstance workflowNodeInstance : workflowNodeInstanceList) {
            save(workflowNodeInstance);
        }
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
    public void updateById(WorkflowNodeInstance workflowNodeInstance) {
        if (Objects.isNull(workflowNodeInstance) || Objects.isNull(workflowNodeInstance.getId())) {
            throw new BizException("节点实例id不能为空");
        }
        if (workflowNodeInstanceMapper.updateById(workflowNodeInstance) != 1) {
            throw new BizException("节点实例更新失败");
        }
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

    @Override
    public void updateNodeForReject(Long nodeInstanceId, String comment) {
        WorkflowNodeInstance workflowNodeInstance = new WorkflowNodeInstance();
        workflowNodeInstance.setId(nodeInstanceId);
        workflowNodeInstance.setStatus(WorkflowNodeInstanceStatusEnum.REJECTED.getCode());
        workflowNodeInstance.setFinishedAt(OperationTimeContext.get());
        workflowNodeInstanceMapper.updateById(workflowNodeInstance);
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
