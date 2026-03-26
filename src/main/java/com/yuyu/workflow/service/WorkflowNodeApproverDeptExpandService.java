package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.WorkflowNodeApproverDeptExpand;

import java.util.List;

/**
 * 工作流节点审批组织展开关系维护服务。
 */
public interface WorkflowNodeApproverDeptExpandService extends IService<WorkflowNodeApproverDeptExpand> {

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
