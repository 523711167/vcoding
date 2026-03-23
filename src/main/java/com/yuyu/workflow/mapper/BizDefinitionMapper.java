package com.yuyu.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyu.workflow.entity.BizDefinition;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 业务定义数据访问组件。
 */
public interface BizDefinitionMapper extends BaseMapper<BizDefinition> {

    /**
     * 按主键软删除业务定义。
     */
    @Update("UPDATE tb_biz_definition SET is_deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND is_deleted = 0")
    int removeById(Long id);

    /**
     * 按主键集合批量软删除业务定义。
     */
    @Update({
            "<script>",
            "UPDATE tb_biz_definition",
            "SET is_deleted = 1, updated_at = CURRENT_TIMESTAMP",
            "WHERE is_deleted = 0 AND id IN ",
            "<foreach collection='idList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeByIds(@Param("idList") List<Long> idList);

    /**
     * 忽略逻辑删除条件查询指定业务编码。
     */
    @Select("SELECT * FROM tb_biz_definition WHERE biz_code = #{bizCode} LIMIT 1")
    BizDefinition selectAnyByBizCode(String bizCode);
}
