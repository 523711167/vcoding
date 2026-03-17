package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.UserDeptRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserDeptRelMapper extends BaseMapper<UserDeptRel> {

    /**
     * 按主键物理删除用户部门关联数据。
     */
    @Delete("DELETE FROM tb_user_dept_rel WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 按主键集合批量物理删除用户部门关联数据。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_user_dept_rel WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int deleteByIds(@Param("idList") List<Long> idList);
}
