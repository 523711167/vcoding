package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.UserRoleDept;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleDeptMapper extends BaseMapper<UserRoleDept> {

    /**
     * 按主键物理删除角色部门关联数据。
     */
    @Delete("DELETE FROM tb_user_role_dept WHERE id = #{id}")
    int removeById(Long id);

    /**
     * 按主键集合批量物理删除角色部门关联数据。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_user_role_dept WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);
}
