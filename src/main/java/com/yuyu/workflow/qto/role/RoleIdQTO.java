package com.yuyu.workflow.qto.role;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "角色主键查询参数")
public class RoleIdQTO extends BaseQueryQTO {
}
