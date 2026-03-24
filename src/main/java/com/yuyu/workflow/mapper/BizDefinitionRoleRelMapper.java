package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.BizDefinitionRoleRel;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 业务定义角色关联数据访问组件。
 */
public interface BizDefinitionRoleRelMapper extends BaseMapper<BizDefinitionRoleRel> {

    /**
     * 按主键软删除业务定义角色关联。
     */
    @Update("UPDATE tb_biz_definition_role_rel SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int removeById(Long id);

    /**
     * 按主键集合批量软删除业务定义角色关联。
     */
    @Update({
            "<script>",
            "UPDATE tb_biz_definition_role_rel",
            "SET is_deleted = 1",
            "WHERE is_deleted = 0 AND id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);

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
