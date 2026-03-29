package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 按主键物理删除用户数据。
     */
    @Delete("DELETE FROM tb_user WHERE id = #{id}")
    int removeById(Long id);

    /**
     * 按主键集合批量物理删除用户数据。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_user WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);

    /**
     * 忽略逻辑删除条件查询指定用户名。
     */
    @Select("SELECT * FROM tb_user WHERE username = #{username} LIMIT 1")
    User selectAnyByUsername(String username);

    /**
     * 查询指定用户名的有效用户。
     */
    @Select("SELECT * FROM tb_user WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    User selectActiveByUsername(String username);


    @Select("""
            SELECT distinct t.*
            FROM tb_user t
            INNER JOIN tb_workflow_node_approver y ON y.approver_value = t.id
            WHERE t.status = 1 AND t.is_deleted = 0 and y.approver_type = 'USER'  and y.node_id = #{workflowNodeId} order by y.sort_order 
            """)
    List<User> selectWorkflowApproverUser(@Param("workflowNodeId") Long workflowNodeId);

    @Select("""
            SELECT distinct u.*
            FROM tb_workflow_node_approver wna
            INNER JOIN tb_user_role_rel urr ON urr.role_id = wna.approver_value
            INNER JOIN tb_user u ON u.id = urr.user_id
            WHERE wna.node_id = #{definitionNodeId}
              AND wna.approver_type = 'ROLE'
              AND u.status = 1
              AND u.is_deleted = 0
            ORDER BY wna.sort_order, wna.id, urr.id, u.id
            """)
    List<User> selectWorkflowApproverRoleUsers(@Param("definitionNodeId") Long definitionNodeId);

    @Select("""
            SELECT distinct u.*
            FROM tb_workflow_node_approver_dept_expand wnade
            INNER JOIN tb_workflow_node_approver wna ON wna.id = wnade.approver_id
            INNER JOIN tb_user_dept_rel_expand udre ON udre.dept_id = wnade.dept_id
            INNER JOIN tb_user u ON u.id = udre.user_id
            WHERE wnade.node_id = #{definitionNodeId}
              AND wna.approver_type = 'DEPT'
              AND wna.is_deleted = 0
              AND u.status = 1
              AND u.is_deleted = 0
            ORDER BY wna.sort_order, wna.id, wnade.distance, wnade.id, udre.id, u.id
            """)
    List<User> selectWorkflowApproverDeptUsers(@Param("definitionNodeId") Long definitionNodeId);

}
