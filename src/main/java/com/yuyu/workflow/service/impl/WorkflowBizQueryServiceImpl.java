package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.DataScopeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.entity.UserDeptRelExpand;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.entity.UserRoleDeptExpand;
import com.yuyu.workflow.mapper.BizApplyMapper;
import com.yuyu.workflow.mapper.UserDeptRelExpandMapper;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.mapper.UserRoleDeptExpandMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverInstanceMapper;
import com.yuyu.workflow.qto.workflow.WorkflowQueryDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryPageQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoPageQTO;
import com.yuyu.workflow.service.WorkflowBizQueryService;
import com.yuyu.workflow.struct.WorkflowQueryStructMapper;
import com.yuyu.workflow.struct.WorkflowTodoStructMapper;
import com.yuyu.workflow.vo.workflow.WorkflowQueryVO;
import com.yuyu.workflow.vo.workflow.WorkflowTodoVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工作流业务查询服务实现。
 */
@Service
public class WorkflowBizQueryServiceImpl implements WorkflowBizQueryService {

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;

    private final BizApplyMapper bizApplyMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserDeptRelMapper userDeptRelMapper;
    private final UserDeptRelExpandMapper userDeptRelExpandMapper;
    private final UserRoleDeptExpandMapper userRoleDeptExpandMapper;
    private final WorkflowNodeApproverInstanceMapper workflowNodeApproverInstanceMapper;
    private final WorkflowQueryStructMapper workflowQueryStructMapper;
    private final WorkflowTodoStructMapper workflowTodoStructMapper;

    /**
     * 注入工作流业务查询依赖。
     */
    public WorkflowBizQueryServiceImpl(BizApplyMapper bizApplyMapper,
                                       UserRoleMapper userRoleMapper,
                                       UserDeptRelMapper userDeptRelMapper,
                                       UserDeptRelExpandMapper userDeptRelExpandMapper,
                                       UserRoleDeptExpandMapper userRoleDeptExpandMapper,
                                       WorkflowNodeApproverInstanceMapper workflowNodeApproverInstanceMapper,
                                       WorkflowQueryStructMapper workflowQueryStructMapper,
                                       WorkflowTodoStructMapper workflowTodoStructMapper) {
        this.bizApplyMapper = bizApplyMapper;
        this.userRoleMapper = userRoleMapper;
        this.userDeptRelMapper = userDeptRelMapper;
        this.userDeptRelExpandMapper = userDeptRelExpandMapper;
        this.userRoleDeptExpandMapper = userRoleDeptExpandMapper;
        this.workflowNodeApproverInstanceMapper = workflowNodeApproverInstanceMapper;
        this.workflowQueryStructMapper = workflowQueryStructMapper;
        this.workflowTodoStructMapper = workflowTodoStructMapper;
    }

    @Override
    public List<WorkflowTodoVO> todoList(WorkflowTodoListQTO qto) {
        requireCurrentUserId(qto.getCurrentUserId());
        List<WorkflowTodoVO> result = workflowNodeApproverInstanceMapper.selectTodoList(qto);
        return workflowTodoStructMapper.toTargetList(result);
    }

    @Override
    public PageVo<WorkflowTodoVO> todoPage(WorkflowTodoPageQTO qto) {
        requireCurrentUserId(qto.getCurrentUserId());
        long pageNum = resolvePageNum(qto.getPageNum());
        long pageSize = resolvePageSize(qto.getPageSize());
        IPage<WorkflowTodoVO> page = new Page<>(pageNum, pageSize);

        IPage<WorkflowTodoVO> resultPage = workflowNodeApproverInstanceMapper.selectTodoPage(page, qto);
        List<WorkflowTodoVO> records = workflowTodoStructMapper.toTargetList(resultPage.getRecords());
        return PageVo.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), records);
    }

    @Override
    public WorkflowTodoVO todoDetail(WorkflowTodoDetailQTO qto) {
        requireCurrentUserId(qto.getCurrentUserId());
        WorkflowTodoVO result = workflowNodeApproverInstanceMapper.selectTodoDetail(qto);
        if (Objects.isNull(result)) {
            throw new BizException("代办记录不存在");
        }
        return workflowTodoStructMapper.toTarget(result);
    }

    @Override
    public List<WorkflowQueryVO> queryList(WorkflowQueryListQTO qto) {
        Long currentUserId = requireCurrentUserId(qto.getCurrentUserId());
        QueryPermission permission = resolveQueryPermission(currentUserId);
        if (Boolean.FALSE.equals(permission.viewAllData) && CollectionUtils.isEmpty(permission.visibleApplicantIdList)) {
            return Collections.emptyList();
        }
        qto.setViewAllData(permission.viewAllData);
        qto.setVisibleApplicantIdList(permission.visibleApplicantIdList);
        return workflowQueryStructMapper.toTargetList(bizApplyMapper.selectQueryList(qto));
    }

    @Override
    public PageVo<WorkflowQueryVO> queryPage(WorkflowQueryPageQTO qto) {
        Long currentUserId = requireCurrentUserId(qto.getCurrentUserId());
        QueryPermission permission = resolveQueryPermission(currentUserId);
        if (Boolean.FALSE.equals(permission.viewAllData) && CollectionUtils.isEmpty(permission.visibleApplicantIdList)) {
            long pageNum = resolvePageNum(qto.getPageNum());
            long pageSize = resolvePageSize(qto.getPageSize());
            return PageVo.of(pageNum, pageSize, 0L, Collections.emptyList());
        }
        qto.setViewAllData(permission.viewAllData);
        qto.setVisibleApplicantIdList(permission.visibleApplicantIdList);

        IPage<WorkflowQueryVO> page = new Page<>(resolvePageNum(qto.getPageNum()), resolvePageSize(qto.getPageSize()));
        IPage<WorkflowQueryVO> resultPage = bizApplyMapper.selectQueryPage(page, qto);
        return PageVo.of(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                workflowQueryStructMapper.toTargetList(resultPage.getRecords())
        );
    }

    @Override
    public WorkflowQueryVO queryDetail(WorkflowQueryDetailQTO qto) {
        Long currentUserId = requireCurrentUserId(qto.getCurrentUserId());
        QueryPermission permission = resolveQueryPermission(currentUserId);
        if (Boolean.FALSE.equals(permission.viewAllData) && CollectionUtils.isEmpty(permission.visibleApplicantIdList)) {
            throw new BizException("无权限查看该记录");
        }
        qto.setViewAllData(permission.viewAllData);
        qto.setVisibleApplicantIdList(permission.visibleApplicantIdList);
        WorkflowQueryVO result = bizApplyMapper.selectQueryDetail(qto);
        if (Objects.isNull(result)) {
            throw new BizException("查询记录不存在或无权限查看");
        }
        return workflowQueryStructMapper.toTarget(result);
    }

    /**
     * 校验当前用户。
     */
    private Long requireCurrentUserId(Long currentUserId) {
        if (Objects.isNull(currentUserId)) {
            throw new BizException("当前用户不能为空");
        }
        return currentUserId;
    }

    /**
     * 规范化页码。
     */
    private long resolvePageNum(Long pageNum) {
        return Objects.nonNull(pageNum) && pageNum > 0 ? pageNum : DEFAULT_PAGE_NUM;
    }

    /**
     * 规范化分页大小。
     */
    private long resolvePageSize(Long pageSize) {
        return Objects.nonNull(pageSize) && pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * 解析查询箱权限：本人发起 + 角色数据权限并集。
     */
    private QueryPermission resolveQueryPermission(Long currentUserId) {
        Set<Long> visibleApplicantIds = new LinkedHashSet<>();
        visibleApplicantIds.add(currentUserId);

        List<Long> enabledRoleIds = userRoleMapper.selectEnabledIdsByUserId(currentUserId);
        if (CollectionUtils.isEmpty(enabledRoleIds)) {
            return new QueryPermission(false, visibleApplicantIds.stream().toList());
        }
        List<UserRole> roleList = userRoleMapper.selectBatchIds(enabledRoleIds);
        if (CollectionUtils.isEmpty(roleList)) {
            return new QueryPermission(false, visibleApplicantIds.stream().toList());
        }

        Set<String> dataScopes = roleList.stream()
                .map(UserRole::getDataScope)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (dataScopes.contains(DataScopeEnum.ALL.getCode())) {
            return new QueryPermission(true, Collections.emptyList());
        }

        if (dataScopes.contains(DataScopeEnum.CURRENT_DEPT.getCode())) {
            visibleApplicantIds.addAll(resolveApplicantIdsByDeptIds(resolveCurrentDeptIds(currentUserId)));
        }
        if (dataScopes.contains(DataScopeEnum.CURRENT_AND_CHILD_DEPT.getCode())) {
            visibleApplicantIds.addAll(resolveApplicantIdsByDeptIds(resolveCurrentAndChildDeptIds(currentUserId)));
        }
        if (dataScopes.contains(DataScopeEnum.CUSTOM_DEPT.getCode())) {
            Set<Long> customRoleIds = roleList.stream()
                    .filter(role -> Objects.equals(role.getDataScope(), DataScopeEnum.CUSTOM_DEPT.getCode()))
                    .map(UserRole::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            visibleApplicantIds.addAll(resolveApplicantIdsByDeptIds(resolveCustomDeptIds(customRoleIds)));
        }
        return new QueryPermission(false, visibleApplicantIds.stream().toList());
    }

    /**
     * 解析当前用户直属组织（主组织+副组织，不展开子组织）。
     */
    private Set<Long> resolveCurrentDeptIds(Long currentUserId) {
        return userDeptRelMapper.selectList(
                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserDeptRel>lambdaQuery()
                                .eq(UserDeptRel::getUserId, currentUserId)
                                .select(UserDeptRel::getDeptId)
                ).stream()
                .map(UserDeptRel::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 解析当前用户组织（主组织+副组织）及其子组织。
     */
    private Set<Long> resolveCurrentAndChildDeptIds(Long currentUserId) {
        return userDeptRelExpandMapper.selectList(
                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserDeptRelExpand>lambdaQuery()
                                .eq(UserDeptRelExpand::getUserId, currentUserId)
                                .select(UserDeptRelExpand::getDeptId)
                ).stream()
                .map(UserDeptRelExpand::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 解析角色自定义组织及其子组织。
     */
    private Set<Long> resolveCustomDeptIds(Set<Long> customRoleIds) {
        if (CollectionUtils.isEmpty(customRoleIds)) {
            return Collections.emptySet();
        }
        return userRoleDeptExpandMapper.selectList(
                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserRoleDeptExpand>lambdaQuery()
                                .in(UserRoleDeptExpand::getRoleId, customRoleIds)
                                .select(UserRoleDeptExpand::getDeptId)
                ).stream()
                .map(UserRoleDeptExpand::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 按组织集合查询申请人主键集合（含主组织+副组织命中）。
     */
    private Set<Long> resolveApplicantIdsByDeptIds(Set<Long> deptIds) {
        if (CollectionUtils.isEmpty(deptIds)) {
            return Collections.emptySet();
        }
        return userDeptRelMapper.selectList(
                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserDeptRel>lambdaQuery()
                                .in(UserDeptRel::getDeptId, deptIds)
                                .select(UserDeptRel::getUserId)
                ).stream()
                .map(UserDeptRel::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 查询权限计算结果。
     */
    private record QueryPermission(boolean viewAllData, List<Long> visibleApplicantIdList) {
    }
}
