package com.yuyu.workflow.service.impl;

import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.common.util.ObjectMapperUtils;
import com.yuyu.workflow.config.JacksonConfig;
import com.yuyu.workflow.entity.WorkflowInstance;
import com.yuyu.workflow.entity.WorkflowNode;
import com.yuyu.workflow.entity.WorkflowTransition;
import com.yuyu.workflow.service.WorkflowDefinitionService;
import com.yuyu.workflow.service.WorkflowLaunchService;
import com.yuyu.workflow.struct.WorkflowLaunchStructMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * 工作流发起服务测试。
 */
class WorkflowLaunchServiceImplTests {

    private WorkflowLaunchService workflowLaunchService;
    private Method findMatchConditionNodeMethod;
    private Constructor<?> auditContextConstructor;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapperUtils objectMapperUtils = new ObjectMapperUtils(new JacksonConfig().objectMapper());
        workflowLaunchService = new WorkflowLaunchServiceImpl(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                objectMapperUtils,
                null,
                mock(WorkflowDefinitionService.class),
                new WorkflowRouteTreeBuilder(),
                mock(WorkflowLaunchStructMapper.class)
        );

        Class<?> auditContextClass =
                Class.forName("com.yuyu.workflow.service.impl.WorkflowLaunchServiceImpl$AuditContext");
        auditContextConstructor = auditContextClass.getDeclaredConstructors()[0];
        auditContextConstructor.setAccessible(true);

        findMatchConditionNodeMethod = WorkflowLaunchServiceImpl.class.getDeclaredMethod(
                "findMatchConditionNode",
                Long.class,
                auditContextClass
        );
        findMatchConditionNodeMethod.setAccessible(true);
    }

    @Test
    void shouldReturnMatchedConditionNode() throws Exception {
        WorkflowTransition hitTransition = buildTransition(1L, 11L, "amount >= 5000", 0);
        WorkflowTransition defaultTransition = buildTransition(2L, 12L, null, 1);

        WorkflowNode hitNode = buildNode(11L, "财务审批", "APPROVAL");
        WorkflowNode defaultNode = buildNode(12L, "结束", "END");

        Object auditContext = buildAuditContext(
                "{\"amount\":6000}",
                Map.of(hitNode.getId(), hitNode, defaultNode.getId(), defaultNode),
                List.of(hitTransition, defaultTransition)
        );

        WorkflowNode result = (WorkflowNode) findMatchConditionNodeMethod.invoke(
                workflowLaunchService,
                1L,
                auditContext
        );

        assertEquals(11L, result.getId());
        assertEquals("财务审批", result.getName());
    }

    @Test
    void shouldFallbackToDefaultConditionNode() throws Exception {
        WorkflowTransition missTransition = buildTransition(1L, 11L, "amount >= 5000", 0);
        WorkflowTransition defaultTransition = buildTransition(2L, 12L, null, 1);

        WorkflowNode missNode = buildNode(11L, "财务审批", "APPROVAL");
        WorkflowNode defaultNode = buildNode(12L, "结束", "END");

        Object auditContext = buildAuditContext(
                "{\"amount\":1000}",
                Map.of(missNode.getId(), missNode, defaultNode.getId(), defaultNode),
                List.of(missTransition, defaultTransition)
        );

        WorkflowNode result = (WorkflowNode) findMatchConditionNodeMethod.invoke(
                workflowLaunchService,
                1L,
                auditContext
        );

        assertEquals(12L, result.getId());
        assertEquals("结束", result.getName());
    }

    @Test
    void shouldThrowWhenNoConditionMatchedAndNoDefaultBranch() throws Exception {
        WorkflowTransition missTransition = buildTransition(1L, 11L, "amount >= 5000", 0);
        WorkflowNode missNode = buildNode(11L, "财务审批", "APPROVAL");

        Object auditContext = buildAuditContext(
                "{\"amount\":1000}",
                Map.of(missNode.getId(), missNode),
                List.of(missTransition)
        );

        Throwable throwable = assertThrows(Throwable.class, () -> findMatchConditionNodeMethod.invoke(
                workflowLaunchService,
                1L,
                auditContext
        ));

        Throwable target = throwable.getCause() == null ? throwable : throwable.getCause();
        assertEquals(BizException.class, target.getClass());
        assertEquals("条件节点未匹配到分支且未配置默认分支", target.getMessage());
    }

    private Object buildAuditContext(String formData,
                                     Map<Long, WorkflowNode> nodeMap,
                                     List<WorkflowTransition> transitionList) throws Exception {
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setFormData(formData);

        return auditContextConstructor.newInstance(
                null,
                workflowInstance,
                null,
                null,
                List.of(),
                null,
                new LinkedHashMap<>(nodeMap),
                Map.of(1L, transitionList)
        );
    }

    private WorkflowTransition buildTransition(Long id, Long toNodeId, String conditionExpr, Integer isDefault) {
        WorkflowTransition transition = new WorkflowTransition();
        transition.setId(id);
        transition.setToNodeId(toNodeId);
        transition.setConditionExpr(conditionExpr);
        transition.setIsDefault(isDefault);
        return transition;
    }

    private WorkflowNode buildNode(Long id, String name, String nodeType) {
        WorkflowNode workflowNode = new WorkflowNode();
        workflowNode.setId(id);
        workflowNode.setName(name);
        workflowNode.setNodeType(nodeType);
        return workflowNode;
    }
}
