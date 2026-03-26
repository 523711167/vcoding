package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.WorkflowInstanceStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.mapper.WorkflowInstanceMapper;
import com.yuyu.workflow.service.WorkflowInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 流程实例服务实现。
 */
@Service
public class WorkflowInstanceServiceImpl implements WorkflowInstanceService {

    private final WorkflowInstanceMapper workflowInstanceMapper;

    /**
     * 注入流程实例服务依赖。
     */
    public WorkflowInstanceServiceImpl(WorkflowInstanceMapper workflowInstanceMapper) {
        this.workflowInstanceMapper = workflowInstanceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(WorkflowInstance workflowInstance) {
        if (Objects.isNull(workflowInstance)) {
            throw new BizException("流程实例不能为空");
        }
        if (workflowInstanceMapper.insert(workflowInstance) != 1) {
            throw new BizException("流程实例保存失败");
        }
    }

    @Override
    public WorkflowInstance getByIdOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        WorkflowInstance instance = workflowInstanceMapper.selectById(id);
        if (Objects.isNull(instance)) {
            throw new BizException("流程实例不存在");
        }
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(WorkflowInstance workflowInstance) {
        if (Objects.isNull(workflowInstance) || Objects.isNull(workflowInstance.getId())) {
            throw new BizException("流程实例id不能为空");
        }
        if (workflowInstanceMapper.updateById(workflowInstance) != 1) {
            throw new BizException("流程实例更新失败");
        }
    }

    @Override
    public void updateNodeForReject(Long instanceId) {
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setId(instanceId);
        workflowInstance.setStatus(WorkflowInstanceStatusEnum.REJECTED.getCode());
        workflowInstance.setFinishedAt(OperationTimeContext.get());
        workflowInstanceMapper.updateById(workflowInstance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByIds(List<Long> idList) {
        List<Long> normalizedIds = normalizeIds(idList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return;
        }
        workflowInstanceMapper.removeByIds(normalizedIds);
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
