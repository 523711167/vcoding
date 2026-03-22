package com.yuyu.workflow.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
