package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.WorkflowApprovalRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批操作记录数据访问组件。
 */
public interface WorkflowApprovalRecordMapper extends BaseMapper<WorkflowApprovalRecord> {

    /**
     * 按主键物理删除审批操作记录。
     */
    @Delete("DELETE FROM tb_workflow_approval_record WHERE id = #{id}")
    int removeById(Long id);

    /**
     * 按主键集合批量物理删除审批操作记录。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_workflow_approval_record WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);
}
