package com.yuyu.workflow.qto.base;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "通用查询参数基类")
public class BaseQueryQTO {

    @Schema(description = "默认单个主键查询参数", example = "1")
    private Long id;

    @ArraySchema(schema = @Schema(description = "默认批量主键查询参数", example = "1"))
    private List<Long> idList;
}
