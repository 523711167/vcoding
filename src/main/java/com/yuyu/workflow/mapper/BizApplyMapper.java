package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.qto.workflow.WorkflowQueryDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryPageQTO;
import com.yuyu.workflow.vo.workflow.WorkflowQueryVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 业务申请数据访问组件。
 */
public interface BizApplyMapper extends BaseMapper<BizApply> {

    /**
     * 按主键软删除业务申请。
     */
    @Update("UPDATE tb_biz_apply SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int removeById(Long id);

    /**
     * 按主键集合批量软删除业务申请。
     */
    @Update({
            "<script>",
            "UPDATE tb_biz_apply",
            "SET is_deleted = 1",
            "WHERE is_deleted = 0 AND id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);

    /**
     * 查询查询箱列表。
     */
    @Select({
            "<script>",
            "SELECT",
            "  ba.id AS biz_apply_id,",
            "  ba.biz_definition_id,",
            "  ba.biz_name,",
            "  ba.title,",
            "  ba.biz_status,",
            "  ba.applicant_id,",
            "  ba.applicant_name,",
            "  ba.dept_id,",
            "  ba.workflow_instance_id,",
            "  wi.status AS workflow_status,",
            "  wi.current_node_name,",
            "  wi.current_node_type,",
            "  ba.form_data,",
            "  ba.submitted_at,",
            "  ba.finished_at,",
            "  ba.updated_at",
            "FROM tb_biz_apply ba",
            "LEFT JOIN tb_workflow_instance wi ON wi.id = ba.workflow_instance_id AND wi.is_deleted = 0",
            "WHERE ba.is_deleted = 0",
            "<if test='qto.bizApplyId != null'>",
            "  AND ba.id = #{qto.bizApplyId}",
            "</if>",
            "<if test='qto.bizDefinitionId != null'>",
            "  AND ba.biz_definition_id = #{qto.bizDefinitionId}",
            "</if>",
            "<if test='qto.title != null and qto.title != \"\"'>",
            "  AND ba.title LIKE CONCAT('%', #{qto.title}, '%')",
            "</if>",
            "<if test='qto.bizStatus != null and qto.bizStatus != \"\"'>",
            "  AND ba.biz_status = #{qto.bizStatus}",
            "</if>",
            "<if test='qto.viewAllData != true'>",
            "  AND ba.applicant_id IN ",
            "  <foreach collection='qto.visibleApplicantIdList' item='applicantId' open='(' separator=',' close=')'>",
            "    #{applicantId}",
            "  </foreach>",
            "</if>",
            "ORDER BY ba.updated_at DESC, ba.id DESC",
            "</script>"
    })
    List<WorkflowQueryVO> selectQueryList(@Param("qto") WorkflowQueryListQTO qto);

    /**
     * 分页查询查询箱列表。
     */
    @Select({
            "<script>",
            "SELECT",
            "  ba.id AS biz_apply_id,",
            "  ba.biz_definition_id,",
            "  ba.biz_name,",
            "  ba.title,",
            "  ba.biz_status,",
            "  ba.applicant_id,",
            "  ba.applicant_name,",
            "  ba.dept_id,",
            "  ba.workflow_instance_id,",
            "  wi.status AS workflow_status,",
            "  wi.current_node_name,",
            "  wi.current_node_type,",
            "  ba.form_data,",
            "  ba.submitted_at,",
            "  ba.finished_at,",
            "  ba.updated_at",
            "FROM tb_biz_apply ba",
            "LEFT JOIN tb_workflow_instance wi ON wi.id = ba.workflow_instance_id AND wi.is_deleted = 0",
            "WHERE ba.is_deleted = 0",
            "<if test='qto.bizApplyId != null'>",
            "  AND ba.id = #{qto.bizApplyId}",
            "</if>",
            "<if test='qto.bizDefinitionId != null'>",
            "  AND ba.biz_definition_id = #{qto.bizDefinitionId}",
            "</if>",
            "<if test='qto.title != null and qto.title != \"\"'>",
            "  AND ba.title LIKE CONCAT('%', #{qto.title}, '%')",
            "</if>",
            "<if test='qto.bizStatus != null and qto.bizStatus != \"\"'>",
            "  AND ba.biz_status = #{qto.bizStatus}",
            "</if>",
            "<if test='qto.viewAllData != true'>",
            "  AND ba.applicant_id IN ",
            "  <foreach collection='qto.visibleApplicantIdList' item='applicantId' open='(' separator=',' close=')'>",
            "    #{applicantId}",
            "  </foreach>",
            "</if>",
            "ORDER BY ba.updated_at DESC, ba.id DESC",
            "</script>"
    })
    IPage<WorkflowQueryVO> selectQueryPage(IPage<WorkflowQueryVO> page, @Param("qto") WorkflowQueryPageQTO qto);

    /**
     * 查询查询箱详情。
     */
    @Select({
            "<script>",
            "SELECT",
            "  ba.id AS biz_apply_id,",
            "  ba.biz_definition_id,",
            "  ba.biz_name,",
            "  ba.title,",
            "  ba.biz_status,",
            "  ba.applicant_id,",
            "  ba.applicant_name,",
            "  ba.dept_id,",
            "  ba.workflow_instance_id,",
            "  wi.status AS workflow_status,",
            "  wi.current_node_name,",
            "  wi.current_node_type,",
            "  ba.form_data,",
            "  ba.submitted_at,",
            "  ba.finished_at,",
            "  ba.updated_at",
            "FROM tb_biz_apply ba",
            "LEFT JOIN tb_workflow_instance wi ON wi.id = ba.workflow_instance_id AND wi.is_deleted = 0",
            "WHERE ba.is_deleted = 0",
            "  AND ba.id = #{qto.bizApplyId}",
            "<if test='qto.viewAllData != true'>",
            "  AND ba.applicant_id IN ",
            "  <foreach collection='qto.visibleApplicantIdList' item='applicantId' open='(' separator=',' close=')'>",
            "    #{applicantId}",
            "  </foreach>",
            "</if>",
            "LIMIT 1",
            "</script>"
    })
    WorkflowQueryVO selectQueryDetail(@Param("qto") WorkflowQueryDetailQTO qto);
}
