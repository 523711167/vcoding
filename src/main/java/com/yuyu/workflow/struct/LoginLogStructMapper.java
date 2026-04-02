package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.entity.LoginLog;
import com.yuyu.workflow.vo.auth.LoginLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

/**
 * 登录日志对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface LoginLogStructMapper {

    /**
     * 构造登录日志实体。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "result", source = "result")
    @Mapping(target = "failReason", source = "failReason")
    @Mapping(target = "clientIp", source = "clientIp")
    @Mapping(target = "userAgent", source = "userAgent")
    @Mapping(target = "loginAt", source = "loginAt")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    LoginLog toEntity(Long userId,
                      String username,
                      String result,
                      String failReason,
                      String clientIp,
                      String userAgent,
                      LocalDateTime loginAt);

    /**
     * 转换登录日志视图对象。
     */
    @Mapping(target = "resultMsg", expression = "java(com.yuyu.workflow.common.enums.LoginResultEnum.getMsgByCode(entity.getResult()))")
    LoginLogVO toVO(LoginLog entity);
}
