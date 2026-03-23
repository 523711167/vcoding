package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.entity.BizDefinitionRoleRel;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.mapper.BizDefinitionRoleRelMapper;
import com.yuyu.workflow.mapper.BizDefinitionMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务定义角色关联服务测试。
 */
@ExtendWith(MockitoExtension.class)
class BizDefinitionRoleRelServiceImplTests {

    @Mock
    private BizDefinitionRoleRelMapper bizDefinitionRoleRelMapper;

    @Mock
    private BizDefinitionMapper bizDefinitionMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    private BizDefinitionRoleRelServiceImpl bizDefinitionRoleRelService;

    @BeforeEach
    void setUp() {
        bizDefinitionRoleRelService = new BizDefinitionRoleRelServiceImpl(
                bizDefinitionRoleRelMapper,
                bizDefinitionMapper,
                userRoleMapper
        );
    }

    /**
     * 全量替换业务绑定角色时应先清理旧关系，再按去重后的角色集合重新写入。
     */
    @Test
    void shouldReplaceRoles() {
        BizDefinition bizDefinition = buildBizDefinition(1L, "LEAVE");
        BizDefinitionRoleRel oldRelation1 = buildRelation(11L, 1L, 3L);
        BizDefinitionRoleRel oldRelation2 = buildRelation(12L, 1L, 5L);

        when(bizDefinitionMapper.selectById(1L)).thenReturn(bizDefinition);
        when(userRoleMapper.selectBatchIds(List.of(3L, 4L))).thenReturn(List.of(buildRole(3L), buildRole(4L)));
        when(bizDefinitionRoleRelMapper.selectList(any())).thenReturn(List.of(oldRelation1, oldRelation2));
        doAnswer(invocation -> {
            BizDefinitionRoleRel relation = invocation.getArgument(0);
            relation.setId(100L);
            return 1;
        }).when(bizDefinitionRoleRelMapper).insert(any(BizDefinitionRoleRel.class));

        bizDefinitionRoleRelService.replaceRoles(1L, Arrays.asList(3L, null, 4L, 3L));

        verify(bizDefinitionRoleRelMapper).removeByIds(List.of(11L, 12L));
        ArgumentCaptor<BizDefinitionRoleRel> captor = ArgumentCaptor.forClass(BizDefinitionRoleRel.class);
        verify(bizDefinitionRoleRelMapper, times(2)).insert(captor.capture());
        assertEquals(List.of(1L, 1L), captor.getAllValues().stream().map(BizDefinitionRoleRel::getBizDefinitionId).toList());
        assertEquals(List.of(3L, 4L), captor.getAllValues().stream().map(BizDefinitionRoleRel::getRoleId).toList());
    }

    /**
     * 替换绑定角色时，若存在无效角色应拒绝写入。
     */
    @Test
    void shouldRejectInvalidRoleWhenReplacingRoles() {
        when(bizDefinitionMapper.selectById(1L)).thenReturn(buildBizDefinition(1L, "LEAVE"));
        when(userRoleMapper.selectBatchIds(List.of(3L, 4L))).thenReturn(List.of(buildRole(3L)));

        BizException exception = assertThrows(BizException.class,
                () -> bizDefinitionRoleRelService.replaceRoles(1L, List.of(3L, 4L)));

        assertEquals("存在无效角色", exception.getMessage());
        verify(bizDefinitionRoleRelMapper, never()).insert(any(BizDefinitionRoleRel.class));
    }

    /**
     * 查询业务绑定角色时应返回当前业务已配置的角色主键集合。
     */
    @Test
    void shouldListRoleIdsByBizCode() {
        when(bizDefinitionMapper.selectById(1L)).thenReturn(buildBizDefinition(1L, "LEAVE"));
        when(bizDefinitionRoleRelMapper.selectList(any())).thenReturn(List.of(
                buildRelation(1L, 1L, 4L),
                buildRelation(2L, 1L, 3L)
        ));

        List<Long> result = bizDefinitionRoleRelService.listRoleIdsByBizDefinitionId(1L);

        assertEquals(List.of(4L, 3L), result);
    }

    /**
     * 删除指定业务定义集合时应清理对应角色关联。
     */
    @Test
    void shouldRemoveRolesByBizIds() {
        when(bizDefinitionRoleRelMapper.selectList(any())).thenReturn(List.of(
                buildRelation(21L, 1L, 3L),
                buildRelation(22L, 2L, 4L)
        ));

        bizDefinitionRoleRelService.removeByBizDefinitionIds(Arrays.asList(1L, 2L, 1L, null));

        verify(bizDefinitionRoleRelMapper).removeByIds(List.of(21L, 22L));
    }

    /**
     * 构造业务定义测试对象。
     */
    private BizDefinition buildBizDefinition(Long id, String bizCode) {
        BizDefinition bizDefinition = new BizDefinition();
        bizDefinition.setId(id);
        bizDefinition.setBizCode(bizCode);
        return bizDefinition;
    }

    /**
     * 构造业务定义角色关联测试对象。
     */
    private BizDefinitionRoleRel buildRelation(Long id, Long bizDefinitionId, Long roleId) {
        BizDefinitionRoleRel relation = new BizDefinitionRoleRel();
        relation.setId(id);
        relation.setBizDefinitionId(bizDefinitionId);
        relation.setRoleId(roleId);
        return relation;
    }

    /**
     * 构造角色测试对象。
     */
    private UserRole buildRole(Long id) {
        UserRole role = new UserRole();
        role.setId(id);
        return role;
    }
}
