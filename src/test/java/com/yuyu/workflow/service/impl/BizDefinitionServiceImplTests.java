package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.RoleCodeEnum;
import com.yuyu.workflow.common.enums.WorkflowDefinitionStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.struct.BizDefinitionStructMapper;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.eto.biz.BizDefinitionCreateETO;
import com.yuyu.workflow.eto.biz.BizDefinitionUpdateETO;
import com.yuyu.workflow.mapper.BizDefinitionMapper;
import com.yuyu.workflow.mapper.BizDefinitionRoleRelMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.mapper.WorkflowDefinitionMapper;
import com.yuyu.workflow.qto.biz.BizDefinitionCurrentUserPageQTO;
import com.yuyu.workflow.qto.biz.BizDefinitionListQTO;
import com.yuyu.workflow.service.BizDefinitionRoleRelService;
import com.yuyu.workflow.vo.biz.BizDefinitionCurrentUserVO;
import com.yuyu.workflow.vo.biz.BizDefinitionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务定义服务测试。
 */
@ExtendWith(MockitoExtension.class)
class BizDefinitionServiceImplTests {

    @Mock
    private BizDefinitionMapper bizDefinitionMapper;

    @Mock
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Mock
    private BizDefinitionRoleRelMapper bizDefinitionRoleRelMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private BizDefinitionRoleRelService bizDefinitionRoleRelService;

    private BizDefinitionServiceImpl bizDefinitionService;

    @BeforeEach
    void setUp() {
        BizDefinitionStructMapper bizDefinitionStructMapper = Mappers.getMapper(BizDefinitionStructMapper.class);
        bizDefinitionService = new BizDefinitionServiceImpl(
                bizDefinitionMapper,
                workflowDefinitionMapper,
                bizDefinitionRoleRelMapper,
                userRoleMapper,
                bizDefinitionStructMapper,
                bizDefinitionRoleRelService
        );
    }

    /**
     * 新增业务定义时应写入创建人并绑定已发布流程。
     */
    @Test
    void shouldCreateBizDefinitionWithPublishedWorkflow() {
        BizDefinitionCreateETO eto = new BizDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setBizCode("LEAVE");
        eto.setBizName("请假申请");
        eto.setBizDesc("请假业务");
        eto.setWorkflowDefinitionCode("LEAVE_APPROVAL");
        eto.setStatus(CommonStatusEnum.ENABLED.getId());
        eto.setRoleIds(List.of(3L, 4L));

        WorkflowDefinition workflowDefinition = buildWorkflowDefinition(100L, "LEAVE_APPROVAL", "请假审批", WorkflowDefinitionStatusEnum.PUBLISHED.getId());
        BizDefinition savedEntity = buildBizDefinition(1L, "LEAVE", "请假申请", "LEAVE_APPROVAL", CommonStatusEnum.ENABLED.getId(), 9L);

        when(bizDefinitionMapper.selectAnyByBizCode("LEAVE")).thenReturn(null);
        when(workflowDefinitionMapper.selectLatestPublishedByCode("LEAVE_APPROVAL")).thenReturn(workflowDefinition);
        when(bizDefinitionMapper.selectById(1L)).thenReturn(savedEntity);
        when(userRoleMapper.selectAnyByCode(RoleCodeEnum.ADMIN.getCode())).thenReturn(buildRole(1L, RoleCodeEnum.ADMIN.getCode()));
        when(workflowDefinitionMapper.selectList(any())).thenReturn(List.of(workflowDefinition));
        doAnswer(invocation -> {
            BizDefinition entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        }).when(bizDefinitionMapper).insert(any(BizDefinition.class));

        BizDefinitionVO result = bizDefinitionService.create(eto);

        ArgumentCaptor<BizDefinition> captor = ArgumentCaptor.forClass(BizDefinition.class);
        verify(bizDefinitionMapper).insert(captor.capture());
        verify(bizDefinitionRoleRelService).replaceRoles(1L, List.of(3L, 4L, 1L));
        assertEquals("LEAVE", captor.getValue().getBizCode());
        assertEquals(9L, captor.getValue().getCreatedBy());
        assertEquals("请假审批", result.getWorkflowDefinitionName());
        assertEquals(CommonStatusEnum.ENABLED.getName(), result.getStatusMsg());
    }

    /**
     * 绑定的流程定义不是已发布状态时应拒绝。
     */
    @Test
    void shouldRejectBindingNonPublishedWorkflowDefinition() {
        BizDefinitionCreateETO eto = new BizDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setBizCode("LEAVE");
        eto.setBizName("请假申请");
        eto.setWorkflowDefinitionCode("LEAVE_APPROVAL");
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        when(bizDefinitionMapper.selectAnyByBizCode("LEAVE")).thenReturn(null);
        when(workflowDefinitionMapper.selectLatestPublishedByCode("LEAVE_APPROVAL")).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> bizDefinitionService.create(eto));

        assertEquals("仅允许绑定存在已发布版本的流程编码", exception.getMessage());
        verify(bizDefinitionMapper, never()).insert(any(BizDefinition.class));
    }

    /**
     * 修改业务定义时应保留原业务编码。
     */
    @Test
    void shouldKeepOriginalBizCodeWhenUpdating() {
        BizDefinitionUpdateETO eto = new BizDefinitionUpdateETO();
        eto.setId(1L);
        eto.setBizName("请假申请-新");
        eto.setBizDesc("新的描述");
        eto.setWorkflowDefinitionCode("LEAVE_APPROVAL_V2");
        eto.setStatus(CommonStatusEnum.DISABLED.getId());
        eto.setRoleIds(List.of(5L, 6L));

        BizDefinition oldEntity = buildBizDefinition(1L, "LEAVE", "请假申请", "LEAVE_APPROVAL", CommonStatusEnum.ENABLED.getId(), 8L);
        WorkflowDefinition workflowDefinition = buildWorkflowDefinition(101L, "LEAVE_APPROVAL_V2", "请假审批V2", WorkflowDefinitionStatusEnum.PUBLISHED.getId());
        BizDefinition updatedEntity = buildBizDefinition(1L, "LEAVE", "请假申请-新", "LEAVE_APPROVAL_V2", CommonStatusEnum.DISABLED.getId(), 8L);
        updatedEntity.setBizDesc("新的描述");

        when(bizDefinitionMapper.selectById(1L)).thenReturn(oldEntity, updatedEntity);
        when(workflowDefinitionMapper.selectLatestPublishedByCode("LEAVE_APPROVAL_V2")).thenReturn(workflowDefinition);
        when(workflowDefinitionMapper.selectList(any())).thenReturn(List.of(workflowDefinition));

        BizDefinitionVO result = bizDefinitionService.update(eto);

        ArgumentCaptor<BizDefinition> captor = ArgumentCaptor.forClass(BizDefinition.class);
        verify(bizDefinitionMapper).updateById(captor.capture());
        verify(bizDefinitionRoleRelService).replaceRoles(1L, List.of(5L, 6L));
        assertEquals("LEAVE", captor.getValue().getBizCode());
        assertEquals(8L, captor.getValue().getCreatedBy());
        assertEquals(CommonStatusEnum.DISABLED.getId(), result.getStatus());
    }

    /**
     * 查询业务定义列表时应补充绑定流程定义信息。
     */
    @Test
    void shouldFillWorkflowDefinitionInfoWhenListing() {
        BizDefinitionListQTO qto = new BizDefinitionListQTO();
        qto.setBizCode("LEAVE");

        BizDefinition entity = buildBizDefinition(1L, "LEAVE", "请假申请", "LEAVE_APPROVAL", CommonStatusEnum.ENABLED.getId(), 9L);
        WorkflowDefinition workflowDefinition = buildWorkflowDefinition(100L, "LEAVE_APPROVAL", "请假审批", WorkflowDefinitionStatusEnum.PUBLISHED.getId());

        when(bizDefinitionMapper.selectList(any())).thenReturn(List.of(entity));
        when(workflowDefinitionMapper.selectList(any())).thenReturn(List.of(workflowDefinition));

        List<BizDefinitionVO> result = bizDefinitionService.list(qto);

        assertEquals(1, result.size());
        assertEquals("LEAVE_APPROVAL", result.get(0).getWorkflowDefinitionCode());
        assertEquals("请假审批", result.get(0).getWorkflowDefinitionName());
    }

    /**
     * 当前用户分页查询可查看业务时应按绑定角色过滤结果。
     */
    @Test
    void shouldPageCurrentUserVisibleBizDefinitionsByRole() {
        BizDefinitionCurrentUserPageQTO qto = new BizDefinitionCurrentUserPageQTO();
        qto.setCurrentUserId(9L);
        qto.setPageNum(1L);
        qto.setPageSize(10L);
        qto.setStatus(CommonStatusEnum.ENABLED.getId());

        BizDefinition entity = buildBizDefinition(1L, "LEAVE", "请假申请", "LEAVE_APPROVAL", CommonStatusEnum.ENABLED.getId(), 9L);
        Page<BizDefinition> page = new Page<>(1L, 10L);
        page.setTotal(1L);
        page.setRecords(List.of(entity));

        when(userRoleMapper.selectEnabledIdsByUserId(9L)).thenReturn(List.of(3L, 4L));
        when(bizDefinitionRoleRelMapper.selectBizDefinitionIdsByRoleIds(List.of(3L, 4L))).thenReturn(List.of(1L, 1L, 2L));
        when(bizDefinitionMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageVo<BizDefinitionCurrentUserVO> result = bizDefinitionService.currentUserPage(qto);

        assertEquals(1L, result.total());
        assertEquals(1, result.records().size());
        assertEquals("LEAVE", result.records().get(0).getBizCode());
        verify(userRoleMapper).selectEnabledIdsByUserId(9L);
        verify(bizDefinitionRoleRelMapper).selectBizDefinitionIdsByRoleIds(List.of(3L, 4L));
    }

    /**
     * ADMIN 角色分页查询可查看业务时也应按业务绑定角色过滤结果。
     */
    @Test
    void shouldPageBizDefinitionsForAdminByRoleBinding() {
        BizDefinitionCurrentUserPageQTO qto = new BizDefinitionCurrentUserPageQTO();
        qto.setCurrentUserId(1L);
        qto.setPageNum(1L);
        qto.setPageSize(10L);

        BizDefinition entity = buildBizDefinition(3L, "MOYU", "客户摸鱼", "MOYU_APPROVAL", CommonStatusEnum.ENABLED.getId(), 1L);
        Page<BizDefinition> page = new Page<>(1L, 10L);
        page.setTotal(1L);
        page.setRecords(List.of(entity));

        when(userRoleMapper.selectEnabledIdsByUserId(1L)).thenReturn(List.of(1L));
        when(bizDefinitionRoleRelMapper.selectBizDefinitionIdsByRoleIds(List.of(1L))).thenReturn(List.of(3L));
        when(bizDefinitionMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageVo<BizDefinitionCurrentUserVO> result = bizDefinitionService.currentUserPage(qto);

        assertEquals(1L, result.total());
        assertEquals(1, result.records().size());
        assertEquals("MOYU", result.records().get(0).getBizCode());
        verify(userRoleMapper).selectEnabledIdsByUserId(1L);
        verify(bizDefinitionRoleRelMapper).selectBizDefinitionIdsByRoleIds(List.of(1L));
    }

    /**
     * 新增业务定义时若未传角色也应自动绑定 ADMIN 角色。
     */
    @Test
    void shouldBindAdminRoleWhenCreatingBizDefinitionWithoutRoleIds() {
        BizDefinitionCreateETO eto = new BizDefinitionCreateETO();
        eto.setCurrentUserId(9L);
        eto.setBizCode("EXPENSE");
        eto.setBizName("报销申请");
        eto.setWorkflowDefinitionCode("EXPENSE_APPROVAL");
        eto.setStatus(CommonStatusEnum.ENABLED.getId());

        WorkflowDefinition workflowDefinition = buildWorkflowDefinition(100L, "EXPENSE_APPROVAL", "报销审批", WorkflowDefinitionStatusEnum.PUBLISHED.getId());
        BizDefinition savedEntity = buildBizDefinition(2L, "EXPENSE", "报销申请", "EXPENSE_APPROVAL", CommonStatusEnum.ENABLED.getId(), 9L);

        when(bizDefinitionMapper.selectAnyByBizCode("EXPENSE")).thenReturn(null);
        when(workflowDefinitionMapper.selectLatestPublishedByCode("EXPENSE_APPROVAL")).thenReturn(workflowDefinition);
        when(userRoleMapper.selectAnyByCode(RoleCodeEnum.ADMIN.getCode())).thenReturn(buildRole(1L, RoleCodeEnum.ADMIN.getCode()));
        when(bizDefinitionMapper.selectById(2L)).thenReturn(savedEntity);
        when(workflowDefinitionMapper.selectList(any())).thenReturn(List.of(workflowDefinition));
        doAnswer(invocation -> {
            BizDefinition entity = invocation.getArgument(0);
            entity.setId(2L);
            return 1;
        }).when(bizDefinitionMapper).insert(any(BizDefinition.class));

        bizDefinitionService.create(eto);

        verify(bizDefinitionRoleRelService).replaceRoles(2L, List.of(1L));
    }

    /**
     * 删除业务定义时应同步清理业务发起权限。
     */
    @Test
    void shouldRemoveInitiatorsWhenDeletingBizDefinition() {
        BizDefinition entity = buildBizDefinition(1L, "LEAVE", "请假申请", "LEAVE_APPROVAL", CommonStatusEnum.ENABLED.getId(), 9L);
        when(bizDefinitionMapper.selectById(1L)).thenReturn(entity);

        bizDefinitionService.delete(List.of(1L));

        verify(bizDefinitionRoleRelService).removeByBizDefinitionIds(List.of(1L));
        verify(bizDefinitionMapper).removeByIds(List.of(1L));
    }

    /**
     * 构造业务定义测试对象。
     */
    private BizDefinition buildBizDefinition(Long id,
                                             String bizCode,
                                             String bizName,
                                             String workflowDefinitionCode,
                                             Integer status,
                                             Long createdBy) {
        BizDefinition entity = new BizDefinition();
        entity.setId(id);
        entity.setBizCode(bizCode);
        entity.setBizName(bizName);
        entity.setWorkflowDefinitionCode(workflowDefinitionCode);
        entity.setStatus(status);
        entity.setCreatedBy(createdBy);
        return entity;
    }

    /**
     * 构造流程定义测试对象。
     */
    private WorkflowDefinition buildWorkflowDefinition(Long id, String code, String name, Integer status) {
        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setStatus(status);
        return entity;
    }

    /**
     * 构造角色测试对象。
     */
    private UserRole buildRole(Long id, String code) {
        UserRole role = new UserRole();
        role.setId(id);
        role.setCode(code);
        return role;
    }
}
