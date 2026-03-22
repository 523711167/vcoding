package com.yuyu.workflow.qto.dept;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "部门主键查询参数")
public class DeptIdQTO extends BaseQueryQTO {
}
