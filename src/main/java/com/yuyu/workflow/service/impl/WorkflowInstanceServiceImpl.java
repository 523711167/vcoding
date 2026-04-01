package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.base.UserContextParam;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.WorkflowInstanceStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.entity.base.BaseIdEntity;
import com.yuyu.workflow.mapper.WorkflowInstanceMapper;
import com.yuyu.workflow.service.WorkflowInstanceService;
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
 * 流程实例服务实现。
 */
@Service
public class WorkflowInstanceServiceImpl extends ServiceImpl<WorkflowInstanceMapper, WorkflowInstance> implements WorkflowInstanceService {

    /**
     * 注入流程实例服务依赖。
     */
    public WorkflowInstanceServiceImpl(WorkflowInstanceMapper workflowInstanceMapper) {
        this.baseMapper = workflowInstanceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WorkflowInstance workflowInstance) {
        if (Objects.isNull(workflowInstance)) {
            throw new BizException("流程实例不能为空");
        }
        if (baseMapper.insert(workflowInstance) != 1) {
            throw new BizException("流程实例保存失败");
        }
        return true;
    }

    @Override
    public WorkflowInstance getByIdOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        WorkflowInstance instance = getById(id);
        if (Objects.isNull(instance)) {
            throw new BizException("流程实例不存在");
        }
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WorkflowInstance workflowInstance) {
        if (Objects.isNull(workflowInstance) || Objects.isNull(workflowInstance.getId())) {
            throw new BizException("流程实例id不能为空");
        }
        if (!super.updateById(workflowInstance)) {
            throw new BizException("流程实例更新失败");
        }
        return true;
    }

    @Override
    public void updateWorkflowInstanceForReject(Long instanceId, WorkflowNodeInstance workflowNodeInstance) {
        baseMapper.update(
                Wrappers.<WorkflowInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, instanceId)
                        .set(WorkflowInstance::getStatus, WorkflowInstanceStatusEnum.REJECTED.getCode())
                        .set(WorkflowInstance::getFinishedAt, OperationTimeContext.get())
                        .set(WorkflowInstance::getCurrentNodeId, workflowNodeInstance.getId())
                        .set(WorkflowInstance::getCurrentNodeType, workflowNodeInstance.getDefinitionNodeType())
                        .set(WorkflowInstance::getCurrentNodeName, workflowNodeInstance.getDefinitionNodeName())
        );
    }

    @Override
    public void updateWorkflowInstanceForApproval(Long instanceId, WorkflowNodeInstance workflowNodeInstance) {
        baseMapper.update(
                Wrappers.<WorkflowInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, instanceId)
                        .set(WorkflowInstance::getStatus, WorkflowInstanceStatusEnum.RUNNING.getCode())
                        .set(WorkflowInstance::getFinishedAt, OperationTimeContext.get())
                        .set(WorkflowInstance::getCurrentNodeId, workflowNodeInstance.getId())
                        .set(WorkflowInstance::getCurrentNodeType, workflowNodeInstance.getDefinitionNodeType())
                        .set(WorkflowInstance::getCurrentNodeName, workflowNodeInstance.getDefinitionNodeName())
        );
    }

    @Override
    public void updateWorkflowInstanceForSite(Long instanceId, WorkflowNodeInstance workflowNodeInstance) {
        baseMapper.update(
                Wrappers.<WorkflowInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, instanceId)
                        .set(WorkflowInstance::getCurrentNodeId, workflowNodeInstance.getDefinitionNodeId())
                        .set(WorkflowInstance::getCurrentNodeType, workflowNodeInstance.getDefinitionNodeType())
                        .set(WorkflowInstance::getCurrentNodeName, workflowNodeInstance.getDefinitionNodeName())
        );
    }

    @Override
    public WorkflowInstance saveStartIntance(BizApply bizApply, WorkflowDefinition workflowDefinition, UserContextParam userContextParam) {
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setBizId(bizApply.getId());
        workflowInstance.setDefinitionId(workflowDefinition.getId());
        workflowInstance.setDefinitionCode(workflowDefinition.getCode());
        workflowInstance.setTitle(bizApply.getTitle());
        workflowInstance.setStatus(WorkflowInstanceStatusEnum.RUNNING.getCode());
        workflowInstance.setApplicantId(bizApply.getApplicantId());
        workflowInstance.setApplicantName(bizApply.getApplicantName());
        workflowInstance.setFormData(bizApply.getFormData());
        workflowInstance.setCurrentNodeId(null);
        workflowInstance.setCurrentNodeName(null);
        workflowInstance.setCurrentNodeType(null);
        workflowInstance.setStartedAt(OperationTimeContext.get());
        super.save(workflowInstance);
        return workflowInstance;
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
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WorkflowInstance> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }
        for (WorkflowInstance entity : entityList) {
            save(entity);
        }
        return true;
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
