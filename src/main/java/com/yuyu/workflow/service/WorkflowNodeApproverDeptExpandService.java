package com.yuyu.workflow.service;

import java.util.List;

/**
 * 工作流节点审批组织展开关系维护服务。
 */
public interface WorkflowNodeApproverDeptExpandService {

    /**
     * 按审批人配置主键集合全量重建展开关系。
     */
    void rebuildByApproverIds(List<Long> approverIds);

    /**
     * 按组织路径集合重建受影响审批配置的展开关系。
     */
    void rebuildByDeptPaths(List<String> deptPaths);

    /**
     * 按审批人配置主键集合删除展开关系。
     */
    void removeByApproverIds(List<Long> approverIds);
}
