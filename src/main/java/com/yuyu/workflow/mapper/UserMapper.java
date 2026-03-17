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
    int deleteById(Long id);

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
    int deleteByIds(@Param("idList") List<Long> idList);

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
}
