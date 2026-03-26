package com.yuyu.workflow.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工作流枚举测试。
 */
class WorkflowEnumTests {

    @Test
    void shouldResolveDefinitionStatusMessage() {
        assertEquals("草稿", WorkflowDefinitionStatusEnum.getMsgById(0));
    }

    @Test
    void shouldResolveNodeTypeMessage() {
        assertEquals("审批节点", WorkflowNodeTypeEnum.getMsgByCode("APPROVAL"));
    }

    @Test
    void shouldCheckApproverTypeExists() {
        assertTrue(WorkflowApproverTypeEnum.containsCode("ROLE"));
    }

    @Test
    void shouldResolveApprovalActionMessage() {
        assertEquals("系统自动审核通过", WorkflowApprovalActionEnum.getMsgByCode("AUTO_APPROVE"));
    }

    @Test
    void shouldCheckNodeInstanceStatusExists() {
        assertTrue(WorkflowNodeInstanceStatusEnum.containsCode("CANCELED"));
        assertFalse(WorkflowNodeInstanceStatusEnum.containsCode("UNKNOWN"));
    }

    @Test
    void shouldResolveApproverInstanceStatusMessage() {
        assertEquals("等待加签", WorkflowNodeApproverInstanceStatusEnum.getMsgByCode("WAITING_ADD_SIGN"));
    }

    @Test
    void shouldResolveWorkflowInstanceStatusMessage() {
        assertEquals("已撤回", WorkflowInstanceStatusEnum.getMsgByCode("CANCELED"));
    }

    @Test
    void shouldResolveApproverRelationTypeMessage() {
        assertEquals("加签审批人", WorkflowNodeApproverRelationTypeEnum.getMsgByCode("ADD_SIGN"));
    }
}
