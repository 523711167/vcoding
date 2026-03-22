package com.yuyu.workflow.eto.base;

import com.yuyu.workflow.common.base.UserContextParam;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Data
@lombok.EqualsAndHashCode(callSuper = true)
@Schema(description = "通用删除参数基类")
public class BaseIdETO extends UserContextParam {

    @Schema(description = "默认单个主键参数", example = "1")
    private Long id;

    @ArraySchema(schema = @Schema(description = "默认批量主键参数", example = "1"))
    private List<Long> idList;

    /**
     * 校验删除请求必须至少包含单个主键或主键集合中的一种。
     */
    @AssertTrue(message = "id和idList不能同时为空")
    public boolean hasDeleteTarget() {
        return Objects.nonNull(id) || !CollectionUtils.isEmpty(idList);
    }
}
