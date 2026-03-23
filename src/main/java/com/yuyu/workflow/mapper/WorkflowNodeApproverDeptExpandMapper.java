package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.WorkflowNodeApproverDeptExpand;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流节点审批组织展开关系数据访问组件。
 */
public interface WorkflowNodeApproverDeptExpandMapper extends BaseMapper<WorkflowNodeApproverDeptExpand> {

    /**
     * 按主键物理删除审批组织展开关系。
     */
    @Delete("DELETE FROM tb_workflow_node_approver_dept_expand WHERE id = #{id}")
    int removeById(Long id);

    /**
     * 按主键集合批量物理删除审批组织展开关系。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_workflow_node_approver_dept_expand WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);
}
