package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.BizDefinitionRoleRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业务定义角色关联数据访问组件。
 */
public interface BizDefinitionRoleRelMapper extends BaseMapper<BizDefinitionRoleRel> {

    /**
     * 按主键物理删除业务定义角色关联。
     */
    @Delete("DELETE FROM tb_biz_definition_role_rel WHERE id = #{id}")
    int removeById(Long id);

    /**
     * 按主键集合批量物理删除业务定义角色关联。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_biz_definition_role_rel WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);

    /**
     * 忽略逻辑删除条件，查询指定业务定义集合的全部角色关联记录。
     */
    @Select({
            "<script>",
            "SELECT *",
            "FROM tb_biz_definition_role_rel",
            "WHERE biz_definition_id IN ",
            "<foreach collection='bizDefinitionIdList' item='bizDefinitionId' open='(' separator=',' close=')'>",
            "#{bizDefinitionId}",
            "</foreach>",
            "</script>"
    })
    List<BizDefinitionRoleRel> selectAnyListByBizDefinitionIds(@Param("bizDefinitionIdList") List<Long> bizDefinitionIdList);

    /**
     * 按角色主键集合查询业务定义主键集合。
     */
    @Select({
            "<script>",
            "SELECT DISTINCT biz_definition_id",
            "FROM tb_biz_definition_role_rel",
            "WHERE is_deleted = 0",
            "AND role_id IN ",
            "<foreach collection='roleIdList' item='roleId' open='(' separator=',' close=')'>",
            "#{roleId}",
            "</foreach>",
            "</script>"
    })
    List<Long> selectBizDefinitionIdsByRoleIds(@Param("roleIdList") List<Long> roleIdList);
}
