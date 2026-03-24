package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.WorkflowNodeInstance;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 节点实例数据访问组件。
 */
public interface WorkflowNodeInstanceMapper extends BaseMapper<WorkflowNodeInstance> {

    /**
     * 按主键软删除节点实例。
     */
    @Update("UPDATE tb_workflow_node_instance SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int removeById(Long id);

    /**
     * 按主键集合批量软删除节点实例。
     */
    @Update({
            "<script>",
            "UPDATE tb_workflow_node_instance",
            "SET is_deleted = 1",
            "WHERE is_deleted = 0 AND id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);
}
