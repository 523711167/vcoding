package com.yuyu.workflow.qto.user;

import com.yuyu.workflow.qto.base.BaseQueryQTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户主键查询参数")
public class UserIdQTO extends BaseQueryQTO {
}
