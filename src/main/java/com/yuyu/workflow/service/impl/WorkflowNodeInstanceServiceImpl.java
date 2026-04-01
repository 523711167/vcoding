package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.WorkflowNodeInstanceStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.entity.base.BaseIdEntity;
import com.yuyu.workflow.mapper.WorkflowNodeInstanceMapper;
import com.yuyu.workflow.service.WorkflowNodeInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 节点实例服务实现。
 */
@Service
public class WorkflowNodeInstanceServiceImpl extends ServiceImpl<WorkflowNodeInstanceMapper, WorkflowNodeInstance> implements WorkflowNodeInstanceService {

    /**
     * 注入节点实例服务依赖。
     */
    public WorkflowNodeInstanceServiceImpl(WorkflowNodeInstanceMapper workflowNodeInstanceMapper) {
        this.baseMapper = workflowNodeInstanceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WorkflowNodeInstance workflowNodeInstance) {
        if (Objects.isNull(workflowNodeInstance)) {
            throw new BizException("节点实例不能为空");
        }
        if (baseMapper.insert(workflowNodeInstance) != 1) {
            throw new BizException("节点实例保存失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WorkflowNodeInstance> workflowNodeInstanceList) {
        if (CollectionUtils.isEmpty(workflowNodeInstanceList)) {
            return true;
        }
        for (WorkflowNodeInstance workflowNodeInstance : workflowNodeInstanceList) {
            save(workflowNodeInstance);
        }
        return true;
    }

    @Override
    public List<WorkflowNodeInstance> listByInstanceIds(List<Long> instanceIdList) {
        List<Long> normalizedIds = normalizeIds(instanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<WorkflowNodeInstance>()
                .in(WorkflowNodeInstance::getInstanceId, normalizedIds)
                .orderByAsc(WorkflowNodeInstance::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WorkflowNodeInstance workflowNodeInstance) {
        if (Objects.isNull(workflowNodeInstance) || Objects.isNull(workflowNodeInstance.getId())) {
            throw new BizException("节点实例id不能为空");
        }
        if (!super.updateById(workflowNodeInstance)) {
            throw new BizException("节点实例更新失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByInstanceIds(List<Long> instanceIdList) {
        List<WorkflowNodeInstance> nodeInstanceList = listByInstanceIds(instanceIdList);
        if (CollectionUtils.isEmpty(nodeInstanceList)) {
            return;
        }
        baseMapper.removeByIds(nodeInstanceList.stream()
                .map(WorkflowNodeInstance::getId)
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
    public void updateNodeInstanceForReject(Long nodeInstanceId) {
        updateNode(nodeInstanceId, WorkflowNodeInstanceStatusEnum.REJECTED);
    }

    @Override
    public void updateNodeInstanceForApprove(Long nodeInstanceId) {
        updateNode(nodeInstanceId, WorkflowNodeInstanceStatusEnum.APPROVED);
    }

    @Override
    public void updateNode(Long nodeInstanceId, WorkflowNodeInstanceStatusEnum nodeInstanceEnum) {
        WorkflowNodeInstance workflowNodeInstance = new WorkflowNodeInstance();
        workflowNodeInstance.setId(nodeInstanceId);
        workflowNodeInstance.setStatus(nodeInstanceEnum.getCode());
        workflowNodeInstance.setFinishedAt(OperationTimeContext.get());
        baseMapper.updateById(workflowNodeInstance);
    }

    @Override
    public WorkflowNodeInstance createOrLoadParallelJoinNodeInstance(WorkflowNode nextNode, Long workflowInstanceId) {
        Optional<WorkflowNodeInstance> exist = getOneOpt(
                Wrappers.<WorkflowNodeInstance>lambdaQuery()
                        .eq(WorkflowNodeInstance::getInstanceId, workflowInstanceId)
                        .eq(WorkflowNodeInstance::getDefinitionNodeId, nextNode.getId())
                        .last("limit 1")
        );

        if (exist.isPresent()) {
            return exist.get();
        }

        WorkflowNodeInstance joinNodeInstance = new WorkflowNodeInstance();
        joinNodeInstance.setInstanceId(workflowInstanceId);
        joinNodeInstance.setDefinitionNodeId(nextNode.getId());
        joinNodeInstance.setDefinitionNodeName(nextNode.getName());
        joinNodeInstance.setDefinitionNodeType(nextNode.getNodeType());
        joinNodeInstance.setParallelBranchRootId(null);
        joinNodeInstance.setStatus(WorkflowNodeInstanceStatusEnum.ACTIVE.getCode());
        joinNodeInstance.setActivatedAt(OperationTimeContext.get());
        joinNodeInstance.setApproveMode(nextNode.getApproveMode());

        super.save(joinNodeInstance);
        return joinNodeInstance;
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

    @Override
    public void activePendingNodeInstance(WorkflowNodeInstance pendingNodeInstance, Long branchRootInstanceId) {
        update(
                Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, pendingNodeInstance.getId())
                        .eq(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.PENDING.getCode())
                        .set(WorkflowNodeInstance::getParallelScopeId, branchRootInstanceId)
                        .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.ACTIVE.getCode())
                        .set(WorkflowNodeInstance::getActivatedAt, OperationTimeContext.get())
        );
    }

    @Override
    public void updateNodeInstanceForEnd(WorkflowNodeInstance pendingNodeInstance) {
        update(
                Wrappers.<WorkflowNodeInstance>lambdaUpdate()
                        .eq(BaseIdEntity::getId, pendingNodeInstance.getId())
                        .set(WorkflowNodeInstance::getStatus, WorkflowNodeInstanceStatusEnum.FINISH.getCode())
                        .set(WorkflowNodeInstance::getFinishedAt, OperationTimeContext.get())
        );
    }

    @Override
    public WorkflowNodeInstance createStartNodeInstance(WorkflowNode startNode, Long workflowInstanceId) {
        WorkflowNodeInstance startNodeInstance = new WorkflowNodeInstance();
        startNodeInstance.setInstanceId(workflowInstanceId);
        startNodeInstance.setDefinitionNodeId(startNode.getId());
        startNodeInstance.setDefinitionNodeName(startNode.getName());
        startNodeInstance.setDefinitionNodeType(startNode.getNodeType());
        startNodeInstance.setParallelBranchRootId(null);
        startNodeInstance.setStatus(WorkflowNodeInstanceStatusEnum.PENDING.getCode());
        super.save(startNodeInstance);
        return startNodeInstance;
    }
}
