package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.UserDeptRelExpand;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserDeptRelExpandMapper extends BaseMapper<UserDeptRelExpand> {

    /**
     * 按主键物理删除用户组织展开关系数据。
     */
    @Delete("DELETE FROM tb_user_dept_rel_expand WHERE id = #{id}")
    int removeById(Long id);

    /**
     * 按主键集合批量物理删除用户组织展开关系数据。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_user_dept_rel_expand WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);
}
