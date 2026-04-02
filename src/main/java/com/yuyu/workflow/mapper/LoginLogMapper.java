package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.LoginLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 登录日志数据访问组件。
 */
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    /**
     * 按主键物理删除登录日志。
     */
    @Delete("DELETE FROM tb_login_log WHERE id = #{id}")
    int removeById(Long id);

    /**
     * 按主键集合批量物理删除登录日志。
     */
    @Delete({
            "<script>",
            "DELETE FROM tb_login_log WHERE id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);
}
