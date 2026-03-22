package com.yuyu.workflow.qto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通用分页查询参数基类")
public class BasePageQTO extends BaseQueryQTO {

    @Schema(description = "页码，默认值为1", example = "1", defaultValue = "1")
    private Long pageNum = 1L;

    @Max(value = 200, message = "pageSize最大为200")
    @Schema(description = "每页条数，默认值为10，最大值为200", example = "10", defaultValue = "10")
    private Long pageSize = 10L;
}
