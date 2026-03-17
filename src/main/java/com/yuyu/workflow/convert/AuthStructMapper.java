package com.yuyu.workflow.convert;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.security.LoginUserDetails;
import com.yuyu.workflow.vo.auth.AuthContextVO;
import com.yuyu.workflow.vo.auth.LoginVO;
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

    /**
     * 组装登录返回对象。
     */
    default LoginVO toLoginVO(String token, Long expireSeconds, LoginUserDetails loginUser) {
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setTokenType("Bearer");
        loginVO.setExpireSeconds(expireSeconds);
        loginVO.setUser(toAuthContextVO(loginUser));
        return loginVO;
    }
}
