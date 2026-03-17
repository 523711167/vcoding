package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.SysMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 按主键物理删除菜单数据。
     */
    @Delete("DELETE FROM tb_sys_menu WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 按主键集合批量物理删除菜单数据。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_sys_menu WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int deleteByIds(@Param("idList") List<Long> idList);

    /**
     * 查询指定用户已启用的权限标识集合。
     */
    @Select({
            "<script>",
            "SELECT DISTINCT m.permission",
            "FROM tb_sys_menu m",
            "INNER JOIN tb_user_role_menu rm ON rm.menu_id = m.id",
            "INNER JOIN tb_user_role r ON r.id = rm.role_id",
            "INNER JOIN tb_user_role_rel ur ON ur.role_id = r.id",
            "WHERE ur.user_id = #{userId}",
            "AND m.permission IS NOT NULL",
            "AND m.permission != ''",
            "AND m.status = 1",
            "AND m.is_deleted = 0",
            "AND r.status = 1",
            "AND r.is_deleted = 0",
            "ORDER BY m.sort_order ASC, m.id ASC",
            "</script>"
    })
    List<String> selectEnabledPermissionsByUserId(@Param("userId") Long userId);
}
