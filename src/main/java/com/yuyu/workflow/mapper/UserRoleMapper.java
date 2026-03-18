package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 按主键物理删除角色数据。
     */
    @Delete("DELETE FROM tb_user_role WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 按主键集合批量物理删除角色数据。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_user_role WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int deleteByIds(@Param("idList") List<Long> idList);

    /**
     * 忽略逻辑删除条件查询指定角色编码。
     */
    @Select("SELECT * FROM tb_user_role WHERE code = #{code} LIMIT 1")
    UserRole selectAnyByCode(String code);

    /**
     * 查询指定用户已启用角色编码集合。
     */
    @Select({
            "<script>",
            "SELECT DISTINCT r.code",
            "FROM tb_user_role r",
            "INNER JOIN tb_user_role_rel rel ON rel.role_id = r.id",
            "WHERE rel.user_id = #{userId}",
            "AND r.status = 1",
            "AND r.is_deleted = 0",
            "</script>"
    })
    List<String> selectEnabledCodesByUserId(@Param("userId") Long userId);
}
