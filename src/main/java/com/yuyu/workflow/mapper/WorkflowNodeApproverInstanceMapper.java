package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.WorkflowNodeApproverInstance;
import com.yuyu.workflow.qto.workflow.WorkflowTodoDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowTodoPageQTO;
import com.yuyu.workflow.vo.workflow.WorkflowTodoVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 查询代办箱列表。
     */
    @Select({
            "<script>",
            "SELECT",
            "  a.id AS approver_instance_id,",
            "  a.node_instance_id,",
            "  a.instance_id AS workflow_instance_id,",
            "  ba.id AS biz_apply_id,",
            "  ba.biz_definition_id,",
            "  ba.biz_name,",
            "  COALESCE(ba.title, wi.title) AS title,",
            "  COALESCE(ba.applicant_id, wi.applicant_id) AS applicant_id,",
            "  COALESCE(ba.applicant_name, wi.applicant_name) AS applicant_name,",
            "  COALESCE(ba.form_data, wi.form_data) AS form_data,",
            "  a.node_name,",
            "  a.node_type,",
            "  a.status AS approver_status,",
            "  wi.started_at,",
            "  a.created_at AS todo_at",
            "FROM tb_workflow_node_approver_instance a",
            "LEFT JOIN tb_workflow_instance wi ON wi.id = a.instance_id AND wi.is_deleted = 0",
            "LEFT JOIN tb_biz_apply ba ON ba.id = wi.biz_id AND ba.is_deleted = 0",
            "WHERE a.approver_id = #{qto.currentUserId}",
            "  AND a.status = 'PENDING'",
            "  AND a.is_active = 1",
            "<if test='qto.bizApplyId != null'>",
            "  AND ba.id = #{qto.bizApplyId}",
            "</if>",
            "<if test='qto.bizDefinitionId != null'>",
            "  AND ba.biz_definition_id = #{qto.bizDefinitionId}",
            "</if>",
            "<if test='qto.title != null and qto.title != \"\"'>",
            "  AND ba.title LIKE CONCAT('%', #{qto.title}, '%')",
            "</if>",
            "ORDER BY a.created_at DESC, a.id DESC",
            "</script>"
    })
    List<WorkflowTodoVO> selectTodoList(@Param("qto") WorkflowTodoListQTO qto);

    /**
     * 查询代办箱分页记录。
     */
    @Select({
            "<script>",
            "SELECT",
            "  a.id AS approver_instance_id,",
            "  a.node_instance_id,",
            "  a.instance_id AS workflow_instance_id,",
            "  ba.id AS biz_apply_id,",
            "  ba.biz_definition_id,",
            "  ba.biz_name,",
            "  COALESCE(ba.title, wi.title) AS title,",
            "  COALESCE(ba.applicant_id, wi.applicant_id) AS applicant_id,",
            "  COALESCE(ba.applicant_name, wi.applicant_name) AS applicant_name,",
            "  COALESCE(ba.form_data, wi.form_data) AS form_data,",
            "  a.node_name,",
            "  a.node_type,",
            "  a.status AS approver_status,",
            "  wi.started_at,",
            "  a.created_at AS todo_at",
            "FROM tb_workflow_node_approver_instance a",
            "LEFT JOIN tb_workflow_instance wi ON wi.id = a.instance_id AND wi.is_deleted = 0",
            "LEFT JOIN tb_biz_apply ba ON ba.id = wi.biz_id AND ba.is_deleted = 0",
            "WHERE a.approver_id = #{qto.currentUserId}",
            "  AND a.status = 'PENDING'",
            "  AND a.is_active = 1",
            "<if test='qto.bizApplyId != null'>",
            "  AND ba.id = #{qto.bizApplyId}",
            "</if>",
            "<if test='qto.bizDefinitionId != null'>",
            "  AND ba.biz_definition_id = #{qto.bizDefinitionId}",
            "</if>",
            "<if test='qto.title != null and qto.title != \"\"'>",
            "  AND ba.title LIKE CONCAT('%', #{qto.title}, '%')",
            "</if>",
            "ORDER BY a.created_at DESC, a.id DESC",
            "</script>"
    })
    IPage<WorkflowTodoVO> selectTodoPage(IPage<WorkflowTodoVO> page,
                                         @Param("qto") WorkflowTodoPageQTO qto);

    /**
     * 查询代办箱详情。
     */
    @Select({
            "<script>",
            "SELECT",
            "  a.id AS approver_instance_id,",
            "  a.node_instance_id,",
            "  a.instance_id AS workflow_instance_id,",
            "  ba.id AS biz_apply_id,",
            "  ba.biz_definition_id,",
            "  ba.biz_name,",
            "  COALESCE(ba.title, wi.title) AS title,",
            "  COALESCE(ba.applicant_id, wi.applicant_id) AS applicant_id,",
            "  COALESCE(ba.applicant_name, wi.applicant_name) AS applicant_name,",
            "  COALESCE(ba.form_data, wi.form_data) AS form_data,",
            "  a.node_name,",
            "  a.node_type,",
            "  a.status AS approver_status,",
            "  wi.started_at,",
            "  a.created_at AS todo_at",
            "FROM tb_workflow_node_approver_instance a",
            "LEFT JOIN tb_workflow_instance wi ON wi.id = a.instance_id AND wi.is_deleted = 0",
            "LEFT JOIN tb_biz_apply ba ON ba.id = wi.biz_id AND ba.is_deleted = 0",
            "WHERE a.approver_id = #{qto.currentUserId}",
            "  AND a.id = #{qto.approverInstanceId}",
            "LIMIT 1",
            "</script>"
    })
    WorkflowTodoVO selectTodoDetail(@Param("qto") WorkflowTodoDetailQTO qto);
}
