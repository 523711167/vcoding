package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.context.OperationTimeContext;
import com.yuyu.workflow.common.enums.WorkflowApproveModeEnum;
import com.yuyu.workflow.common.enums.WorkflowNodeApproverInstanceStatusEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import com.yuyu.workflow.eto.workflow.WorkflowRejectAuditETO;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverInstanceMapper;
import com.yuyu.workflow.service.UserService;
import com.yuyu.workflow.service.WorkflowNodeApproverDeptExpandService;
import com.yuyu.workflow.service.WorkflowNodeApproverInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 节点审批人实例服务实现。
 */
@Service
public class WorkflowNodeApproverInstanceServiceImpl extends ServiceImpl<WorkflowNodeApproverInstanceMapper, WorkflowNodeApproverInstance> implements WorkflowNodeApproverInstanceService {

    private final WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService;
    private final WorkflowNodeApproverDeptExpandService workflowNodeApproverDeptExpandService;
    private final UserService userService;

    /**
     * 注入节点审批人实例服务依赖。
     */
    public WorkflowNodeApproverInstanceServiceImpl(WorkflowNodeApproverInstanceMapper workflowNodeApproverInstanceMapper,
                                                   WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService,
                                                   WorkflowNodeApproverDeptExpandService workflowNodeApproverDeptExpandService,
                                                   com.yuyu.workflow.service.WorkflowNodeApproverService workflowNodeApproverService,
                                                   com.yuyu.workflow.mapper.UserRoleRelMapper userRoleRelMapper,
                                                   UserService userService) {
        this.baseMapper = workflowNodeApproverInstanceMapper;
        this.workflowNodeApproverInstanceService = workflowNodeApproverInstanceService;
        this.workflowNodeApproverDeptExpandService = workflowNodeApproverDeptExpandService;
        this.userService = userService;
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
        updateNodeApprover(nodeInstanceId, comment, approverInstanceId, WorkflowNodeApproverInstanceStatusEnum.REJECTED);
    }

    @Override
    public void updateNodeApproverForApprove(Long nodeInstanceId, String comment, Long approverInstanceId) {
        updateNodeApprover(nodeInstanceId, comment, approverInstanceId, WorkflowNodeApproverInstanceStatusEnum.APPROVED);
    }

    @Override
    public void updateNodeApprover(Long nodeInstanceId, String comment, Long approverInstanceId, WorkflowNodeApproverInstanceStatusEnum approverEnum) {
        WorkflowNodeApproverInstance update = new WorkflowNodeApproverInstance();
        update.setStatus(approverEnum.getCode());
        update.setFinishedAt(OperationTimeContext.get());
        update.setComment(comment);
        int i = baseMapper.update(update, new LambdaQueryWrapper<WorkflowNodeApproverInstance>()
                .eq(WorkflowNodeApproverInstance::getNodeInstanceId, nodeInstanceId)
                .eq(WorkflowNodeApproverInstance::getId, approverInstanceId));
        if (i == 0) {
            throw new BizException("审批人实例更新失败");
        }
    }

    @Override
    public boolean activateNextApproverInstance(WorkflowNodeApproverInstance current, List<WorkflowNodeApproverInstance> approverInstanceList) {
        Optional<WorkflowNodeApproverInstance> nextApproverOpt =
                approverInstanceList.stream()
                        .filter(item -> item.getSortOrder() != null && current.getSortOrder() != null)
                        .filter(item -> item.getSortOrder() > current.getSortOrder())
                        .findFirst();

        if (nextApproverOpt.isPresent()) {
            WorkflowNodeApproverInstance nextApprover = nextApproverOpt.get();

            update(
                    new LambdaUpdateWrapper<WorkflowNodeApproverInstance>()
                            .eq(WorkflowNodeApproverInstance::getId, nextApprover.getId())
                            .set(WorkflowNodeApproverInstance::getIsActive, YesNoEnum.YES.getId())
            );
            // 还有下一位，当前节点不能算通过
            return false;
        }
        // 没有下一位，说明顺签完成
        return true;
    }


    @Override
    public void cancelOtherPendingApprovers(Long instanceId, Long nodeInstanceId, Long approverInstanceId) {
        update(
                Wrappers.<WorkflowNodeApproverInstance>lambdaUpdate()
                        .eq(WorkflowNodeApproverInstance::getInstanceId, instanceId)
                        .eq(WorkflowNodeApproverInstance::getNodeInstanceId, nodeInstanceId)
                        .ne(WorkflowNodeApproverInstance::getId, approverInstanceId)
                        .eq(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode())
                        .set(WorkflowNodeApproverInstance::getFinishedAt, OperationTimeContext.get())
                        .set(WorkflowNodeApproverInstance::getStatus, WorkflowNodeApproverInstanceStatusEnum.CANCELED.getCode())
        );
    }

    @Override
    public void saveApproverInstancesFordept(WorkflowNodeInstance workflowNodeInstance) {
        String approveMode = workflowNodeInstance.getApproveMode();
        List<User> userList = ((UserMapper) userService.getBaseMapper())
                .selectWorkflowApproverDeptUsers(workflowNodeInstance.getDefinitionNodeId());
        if (CollectionUtils.isEmpty(userList)) {
            return;
        }
        saveUserList(workflowNodeInstance, userList, approveMode);
    }

    @Override
    public void saveApproverInstancesForRole(WorkflowNodeInstance workflowNodeInstance) {
        String approveMode = workflowNodeInstance.getApproveMode();
        List<User> userList = ((UserMapper) userService.getBaseMapper())
                .selectWorkflowApproverRoleUsers(workflowNodeInstance.getDefinitionNodeId());
        if (CollectionUtils.isEmpty(userList)) {
            return;
        }

        saveUserList(workflowNodeInstance, userList, approveMode);
    }

    @Override
    public void saveApproverInstancesForUser(WorkflowNodeInstance workflowNodeInstance) {
        String approveMode = workflowNodeInstance.getApproveMode();
        List<User> userList = ((UserMapper) userService.getBaseMapper()).selectWorkflowApproverUser(workflowNodeInstance.getId());
        if (CollectionUtils.isEmpty(userList)) {
            return;
        }

        saveUserList(workflowNodeInstance, userList, approveMode);
    }

    private void saveUserList(WorkflowNodeInstance workflowNodeInstance, List<User> userList, String approveMode) {
        List<User> deduplicatedUserList = userList.stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.nonNull(item.getId()))
                .collect(java.util.stream.Collectors.toMap(
                        User::getId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
        List<WorkflowNodeApproverInstance> nodeApproverInstanceList =
                IntStream.range(0, deduplicatedUserList.size())
                        .mapToObj(index -> {
                            User item = deduplicatedUserList.get(index);
                            WorkflowNodeApproverInstance workflowNodeApproverInstance = new WorkflowNodeApproverInstance();
                            workflowNodeApproverInstance.setNodeInstanceId(workflowNodeInstance.getId());
                            workflowNodeApproverInstance.setInstanceId(workflowNodeInstance.getInstanceId());
                            workflowNodeApproverInstance.setApproverId(item.getId());
                            workflowNodeApproverInstance.setApproverName(item.getRealName());
                            workflowNodeApproverInstance.setNodeName(workflowNodeInstance.getDefinitionNodeName());
                            workflowNodeApproverInstance.setNodeType(workflowNodeInstance.getDefinitionNodeType());
                            workflowNodeApproverInstance.setSortOrder(index + 1);
                            workflowNodeApproverInstance.setStatus(WorkflowNodeApproverInstanceStatusEnum.PENDING.getCode());
                            if (WorkflowApproveModeEnum.isSequential(approveMode) && index > 0) {
                                workflowNodeApproverInstance.setIsActive(YesNoEnum.NO.getId());
                            } else {
                                workflowNodeApproverInstance.setIsActive(YesNoEnum.YES.getId());
                            }
                            workflowNodeApproverInstance.setCreatedAt(OperationTimeContext.get());
                            return workflowNodeApproverInstance;
                        })
                        .toList();

        super.saveBatch(nodeApproverInstanceList);
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
