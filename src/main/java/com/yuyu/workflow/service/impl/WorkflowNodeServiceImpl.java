package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import com.yuyu.workflow.service.WorkflowNodeService;
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
 * 流程节点服务实现。
 */
@Service
public class WorkflowNodeServiceImpl extends ServiceImpl<WorkflowNodeMapper, WorkflowNode> implements WorkflowNodeService {

    /**
     * 注入流程节点服务依赖。
     */
    public WorkflowNodeServiceImpl(WorkflowNodeMapper workflowNodeMapper) {
        this.baseMapper = workflowNodeMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WorkflowNode workflowNode) {
        if (Objects.isNull(workflowNode)) {
            throw new BizException("流程节点不能为空");
        }
        if (baseMapper.insert(workflowNode) != 1) {
            throw new BizException("流程节点保存失败");
        }
        return true;
    }

    @Override
    public WorkflowNode getByIdOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        WorkflowNode workflowNode = getById(id);
        if (Objects.isNull(workflowNode)) {
            throw new BizException("流程节点不存在");
        }
        return workflowNode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WorkflowNode workflowNode) {
        if (Objects.isNull(workflowNode) || Objects.isNull(workflowNode.getId())) {
            throw new BizException("流程节点id不能为空");
        }
        if (!super.updateById(workflowNode)) {
            throw new BizException("流程节点更新失败");
        }
        return true;
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
