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
import com.yuyu.workflow.qto.workflow.WorkflowQueryDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryPageQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoPageQTO;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.service.RoleService;
import com.yuyu.workflow.service.UserDeptRelExpandService;
import com.yuyu.workflow.service.UserDeptRelService;
import com.yuyu.workflow.service.UserRoleDeptExpandService;
import com.yuyu.workflow.service.WorkflowBizQueryService;
import com.yuyu.workflow.service.WorkflowNodeApproverInstanceService;
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
import java.util.stream.Collectors;

/**
 * 工作流业务查询服务实现。
 */
@Service
public class WorkflowBizQueryServiceImpl implements WorkflowBizQueryService {

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;

    private final BizApplyService bizApplyService;
    private final RoleService roleService;
    private final UserDeptRelService userDeptRelService;
    private final UserDeptRelExpandService userDeptRelExpandService;
    private final UserRoleDeptExpandService userRoleDeptExpandService;
    private final WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService;
    private final WorkflowQueryStructMapper workflowQueryStructMapper;
    private final WorkflowTodoStructMapper workflowTodoStructMapper;

    /**
     * 注入工作流业务查询依赖。
     */
    public WorkflowBizQueryServiceImpl(BizApplyService bizApplyService,
                                       RoleService roleService,
                                       UserDeptRelService userDeptRelService,
                                       UserDeptRelExpandService userDeptRelExpandService,
                                       UserRoleDeptExpandService userRoleDeptExpandService,
                                       WorkflowNodeApproverInstanceService workflowNodeApproverInstanceService,
                                       WorkflowQueryStructMapper workflowQueryStructMapper,
                                       WorkflowTodoStructMapper workflowTodoStructMapper) {
        this.bizApplyService = bizApplyService;
        this.roleService = roleService;
        this.userDeptRelService = userDeptRelService;
        this.userDeptRelExpandService = userDeptRelExpandService;
        this.userRoleDeptExpandService = userRoleDeptExpandService;
        this.workflowNodeApproverInstanceService = workflowNodeApproverInstanceService;
        this.workflowQueryStructMapper = workflowQueryStructMapper;
        this.workflowTodoStructMapper = workflowTodoStructMapper;
    }

    @Override
    public List<WorkflowTodoVO> todoList(WorkflowTodoListQTO qto) {
        requireCurrentUserId(qto.getCurrentUserId());
        List<WorkflowTodoVO> result = workflowNodeApproverInstanceService.listTodos(qto);
        return workflowTodoStructMapper.toTargetList(result);
    }

    @Override
    public PageVo<WorkflowTodoVO> todoPage(WorkflowTodoPageQTO qto) {
        requireCurrentUserId(qto.getCurrentUserId());
        long pageNum = resolvePageNum(qto.getPageNum());
        long pageSize = resolvePageSize(qto.getPageSize());
        IPage<WorkflowTodoVO> page = new Page<>(pageNum, pageSize);

        IPage<WorkflowTodoVO> resultPage = workflowNodeApproverInstanceService.pageTodos(page, qto);
        List<WorkflowTodoVO> records = workflowTodoStructMapper.toTargetList(resultPage.getRecords());
        return PageVo.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), records);
    }

    @Override
    public WorkflowTodoVO todoDetail(WorkflowTodoDetailQTO qto) {
        requireCurrentUserId(qto.getCurrentUserId());
        WorkflowTodoVO result = workflowNodeApproverInstanceService.detailTodo(qto);
        if (Objects.isNull(result)) {
            throw new BizException("代办记录不存在");
        }
        return workflowTodoStructMapper.toTarget(result);
    }

    @Override
    public List<WorkflowTodoVO> processedList(WorkflowTodoListQTO qto) {
        requireCurrentUserId(qto.getCurrentUserId());
        List<WorkflowTodoVO> result = workflowNodeApproverInstanceService.listProcessed(qto);
        return workflowTodoStructMapper.toTargetList(result);
    }

    @Override
    public PageVo<WorkflowTodoVO> processedPage(WorkflowTodoPageQTO qto) {
        requireCurrentUserId(qto.getCurrentUserId());
        long pageNum = resolvePageNum(qto.getPageNum());
        long pageSize = resolvePageSize(qto.getPageSize());
        IPage<WorkflowTodoVO> page = new Page<>(pageNum, pageSize);

        IPage<WorkflowTodoVO> resultPage = workflowNodeApproverInstanceService.pageProcessed(page, qto);
        List<WorkflowTodoVO> records = workflowTodoStructMapper.toTargetList(resultPage.getRecords());
        return PageVo.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), records);
    }

    @Override
    public WorkflowTodoVO processedDetail(WorkflowTodoDetailQTO qto) {
        requireCurrentUserId(qto.getCurrentUserId());
        WorkflowTodoVO result = workflowNodeApproverInstanceService.detailProcessed(qto);
        if (Objects.isNull(result)) {
            throw new BizException("已办记录不存在");
        }
        return workflowTodoStructMapper.toTarget(result);
    }

    @Override
    public List<WorkflowQueryVO> queryList(WorkflowQueryListQTO qto) {
        Long currentUserId = requireCurrentUserId(qto.getCurrentUserId());
        QueryPermission permission = resolveQueryPermission(currentUserId);
        qto.setHasAllData(permission.hasAllData);
        qto.setVisibleDeptIdList(permission.visibleDeptIdList);
        return workflowQueryStructMapper.toTargetList(bizApplyService.listQueries(qto));
    }

    @Override
    public PageVo<WorkflowQueryVO> queryPage(WorkflowQueryPageQTO qto) {
        Long currentUserId = requireCurrentUserId(qto.getCurrentUserId());
        QueryPermission permission = resolveQueryPermission(currentUserId);
        qto.setHasAllData(permission.hasAllData);
        qto.setVisibleDeptIdList(permission.visibleDeptIdList);

        IPage<WorkflowQueryVO> page = new Page<>(resolvePageNum(qto.getPageNum()), resolvePageSize(qto.getPageSize()));
        IPage<WorkflowQueryVO> resultPage = bizApplyService.pageQueries(page, qto);
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
        qto.setHasAllData(permission.hasAllData);
        qto.setVisibleDeptIdList(permission.visibleDeptIdList);
        WorkflowQueryVO result = bizApplyService.detailQuery(qto);
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
     * 解析查询箱权限：按当前用户角色数据权限计算组织范围，用于 SQL 的本人 + 组织交集判断。
     */
    private QueryPermission resolveQueryPermission(Long currentUserId) {
        List<Long> enabledRoleIds = roleService.listEnabledRoleIdsByUserId(currentUserId);
        if (CollectionUtils.isEmpty(enabledRoleIds)) {
            return new QueryPermission(false, Collections.emptyList());
        }
        List<UserRole> roleList = roleService.listByIds(enabledRoleIds);
        if (CollectionUtils.isEmpty(roleList)) {
            return new QueryPermission(false, Collections.emptyList());
        }

        Set<String> dataScopes = roleList.stream()
                .map(UserRole::getDataScope)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (dataScopes.contains(DataScopeEnum.ALL.getCode())) {
            return new QueryPermission(true, Collections.emptyList());
        }
        return new QueryPermission(false, resolveVisibleDeptIds(currentUserId, roleList, dataScopes).stream().toList());
    }

    /**
     * 计算查询箱可见组织范围。
     */
    private Set<Long> resolveVisibleDeptIds(Long currentUserId, List<UserRole> roleList, Set<String> dataScopes) {
        ScopeFlags scopeFlags = ScopeFlags.from(dataScopes);

        // 仅本人数据：不放开任何组织范围，最终由 SQL 中 applicant_id = currentUserId 兜底。
        if (scopeFlags.selfOnly()) {
            return Collections.emptySet();
        }

        Set<Long> baseDeptIds = resolveBaseDeptIds(currentUserId, scopeFlags);
        if (!scopeFlags.hasCustomDept()) {
            return baseDeptIds;
        }

        Set<Long> customDeptIds = resolveCustomDeptIdsByRoleList(roleList);
        return mergeBaseAndCustomDeptIds(baseDeptIds, customDeptIds);
    }

    /**
     * 计算“当前组织口径”的基础组织范围。
     */
    private Set<Long> resolveBaseDeptIds(Long currentUserId, ScopeFlags scopeFlags) {
        if (scopeFlags.hasCurrentAndChildDept()) {
            return resolveCurrentAndChildDeptIds(currentUserId);
        }
        if (scopeFlags.hasCurrentDept()) {
            return resolveCurrentDeptIds(currentUserId);
        }
        return Collections.emptySet();
    }

    /**
     * 解析角色自定义组织范围。
     */
    private Set<Long> resolveCustomDeptIdsByRoleList(List<UserRole> roleList) {
        Set<Long> customRoleIds = roleList.stream()
                .filter(role -> Objects.equals(role.getDataScope(), DataScopeEnum.CUSTOM_DEPT.getCode()))
                .map(UserRole::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return resolveCustomDeptIds(customRoleIds);
    }

    /**
     * 合并“基础组织范围”与“自定义组织范围”。
     */
    private Set<Long> mergeBaseAndCustomDeptIds(Set<Long> baseDeptIds, Set<Long> customDeptIds) {
        if (CollectionUtils.isEmpty(baseDeptIds)) {
            return new LinkedHashSet<>(customDeptIds);
        }
        // 只要出现 CUSTOM_DEPT + 组织范围组合，统一按交集收敛。
        return intersectDeptIds(baseDeptIds, customDeptIds);
    }

    /**
     * 解析当前用户直属组织（主组织+副组织，不展开子组织）。
     */
    private Set<Long> resolveCurrentDeptIds(Long currentUserId) {
        return userDeptRelService.list(
                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserDeptRel>query()
                                .eq("user_id", currentUserId)
                                .select("dept_id")
                ).stream()
                .map(UserDeptRel::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 解析当前用户组织（主组织+副组织）及其子组织。
     */
    private Set<Long> resolveCurrentAndChildDeptIds(Long currentUserId) {
        return userDeptRelExpandService.list(
                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserDeptRelExpand>query()
                                .eq("user_id", currentUserId)
                                .select("dept_id")
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
        return userRoleDeptExpandService.list(
                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserRoleDeptExpand>query()
                                .in("role_id", customRoleIds)
                                .select("dept_id")
                ).stream()
                .map(UserRoleDeptExpand::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> intersectDeptIds(Set<Long> left, Set<Long> right) {
        if (CollectionUtils.isEmpty(left) || CollectionUtils.isEmpty(right)) {
            return Collections.emptySet();
        }
        Set<Long> rightSet = new LinkedHashSet<>(right);
        return left.stream()
                .filter(rightSet::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 查询权限计算结果。
     */
    private record QueryPermission(boolean hasAllData, List<Long> visibleDeptIdList) {
    }

    /**
     * 数据权限类型标记。
     */
    private record ScopeFlags(boolean hasSelf,
                              boolean hasCurrentDept,
                              boolean hasCurrentAndChildDept,
                              boolean hasCustomDept) {

        private static ScopeFlags from(Set<String> dataScopes) {
            return new ScopeFlags(
                    dataScopes.contains(DataScopeEnum.SELF.getCode()),
                    dataScopes.contains(DataScopeEnum.CURRENT_DEPT.getCode()),
                    dataScopes.contains(DataScopeEnum.CURRENT_AND_CHILD_DEPT.getCode()),
                    dataScopes.contains(DataScopeEnum.CUSTOM_DEPT.getCode())
            );
        }

        private boolean selfOnly() {
            return hasSelf && !hasCurrentDept && !hasCurrentAndChildDept && !hasCustomDept;
        }
    }
}
