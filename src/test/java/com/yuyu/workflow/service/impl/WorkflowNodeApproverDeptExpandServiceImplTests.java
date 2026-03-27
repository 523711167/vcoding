package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowNodeApprover;
import com.yuyu.workflow.entity.WorkflowNodeApproverDeptExpand;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverDeptExpandMapper;
import com.yuyu.workflow.mapper.WorkflowNodeApproverMapper;
import com.yuyu.workflow.mapper.WorkflowNodeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作流节点审批组织展开关系重建测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowNodeApproverDeptExpandServiceImplTests {

    @Mock
    private WorkflowNodeApproverMapper workflowNodeApproverMapper;

    @Mock
    private WorkflowNodeApproverDeptExpandMapper workflowNodeApproverDeptExpandMapper;

    @Mock
    private WorkflowNodeMapper workflowNodeMapper;

    @Mock
    private UserDeptMapper userDeptMapper;

    @InjectMocks
    private WorkflowNodeApproverDeptExpandServiceImpl workflowNodeApproverDeptExpandService;

    /**
     * 重建展开关系时应写入自身和全部有效子孙节点。
     */
    @Test
    void shouldRebuildDeptApproverSelfAndDescendantRelations() {
        WorkflowNodeApprover approver = new WorkflowNodeApprover();
        approver.setId(100L);
        approver.setDefinitionId(1L);
        approver.setNodeId(10L);
        approver.setApproverType("DEPT");
        approver.setApproverValue(2L);

        WorkflowNode node = new WorkflowNode();
        node.setId(10L);
        node.setDefinitionId(1L);

        UserDept sourceDept = buildDept(2L, 1L, "DEPT", null, "/1/2/");
        UserDept childDept = buildDept(3L, 2L, "DEPT", null, "/1/2/3/");
        UserDept postDept = buildDept(4L, 3L, "POST", "JAVA", "/1/2/3/4/");

        when(workflowNodeApproverDeptExpandMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(workflowNodeApproverMapper.selectList(any())).thenReturn(List.of(approver));
        when(workflowNodeMapper.selectList(any())).thenReturn(List.of(node));
        when(userDeptMapper.selectList(any())).thenReturn(
                List.of(sourceDept),
                List.of(sourceDept, childDept, postDept)
        );

        workflowNodeApproverDeptExpandService.rebuildByApproverIds(List.of(100L));

        ArgumentCaptor<WorkflowNodeApproverDeptExpand> captor = ArgumentCaptor.forClass(WorkflowNodeApproverDeptExpand.class);
        verify(workflowNodeApproverDeptExpandMapper, times(3)).insert(captor.capture());
        List<WorkflowNodeApproverDeptExpand> expandRelations = captor.getAllValues();

        assertEquals("SELF", expandRelations.get(0).getRelationType());
        assertEquals(0, expandRelations.get(0).getDistance());
        assertEquals(Long.valueOf(2L), expandRelations.get(0).getDeptId());
        assertEquals(Long.valueOf(1L), expandRelations.get(0).getDefinitionId());

        assertEquals("DESCENDANT", expandRelations.get(1).getRelationType());
        assertEquals(1, expandRelations.get(1).getDistance());
        assertEquals(Long.valueOf(3L), expandRelations.get(1).getDeptId());

        assertEquals("DESCENDANT", expandRelations.get(2).getRelationType());
        assertEquals(2, expandRelations.get(2).getDistance());
        assertEquals(Long.valueOf(4L), expandRelations.get(2).getDeptId());
        assertEquals("JAVA", expandRelations.get(2).getPostType());
    }

    /**
     * 构造组织测试对象。
     */
    private UserDept buildDept(Long id, Long parentId, String orgType, String postType, String path) {
        UserDept dept = new UserDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setOrgType(orgType);
        dept.setPostType(postType);
        dept.setPath(path);
        dept.setStatus(CommonStatusEnum.ENABLED.getId());
        return dept;
    }
}
