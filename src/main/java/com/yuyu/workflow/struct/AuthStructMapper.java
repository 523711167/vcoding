package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.security.LoginUserDetails;
import com.yuyu.workflow.vo.auth.AuthContextVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 认证模块对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface AuthStructMapper {

    /**
     * 将登录用户上下文转换为当前登录用户视图。
     */
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "statusMsg", expression = "java(com.yuyu.workflow.common.enums.CommonStatusEnum.getMsgById(loginUser.getStatus()))")
    AuthContextVO toAuthContextVO(LoginUserDetails loginUser);
}
