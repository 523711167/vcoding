package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 节点审批人实例数据访问组件。
 */
public interface WorkflowNodeApproverInstanceMapper extends BaseMapper<WorkflowNodeApproverInstance> {

    /**
     * 按主键物理删除节点审批人实例。
     */
    @Delete("DELETE FROM tb_workflow_node_approver_instance WHERE id = #{id}")
    int removeById(Long id);

    /**
     * 按主键集合批量物理删除节点审批人实例。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_workflow_node_approver_instance WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);
}
