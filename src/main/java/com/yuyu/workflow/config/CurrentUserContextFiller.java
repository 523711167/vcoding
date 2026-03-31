package com.yuyu.workflow.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.common.base.UserContextParam;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.security.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * 统一填充当前登录用户上下文信息。
 */
@Component
public class CurrentUserContextFiller {

    private final UserDeptRelMapper userDeptRelMapper;

    /**
     * 注入用户组织关系查询组件。
     */
    public CurrentUserContextFiller(UserDeptRelMapper userDeptRelMapper) {
        this.userDeptRelMapper = userDeptRelMapper;
    }

    /**
     * 向参数对象写入当前登录人基础上下文。
     */
    public void fill(UserContextParam userContextParam) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userContextParam.setCurrentUserId(currentUserId);
        userContextParam.setCurrentUsername(SecurityUtils.getCurrentUsername());
        userContextParam.setCurrentPrimaryDeptId(findCurrentPrimaryDeptId(currentUserId));
    }

    /**
     * 查询当前登录人的主组织ID；未配置主组织时返回 null。
     */
    private Long findCurrentPrimaryDeptId(Long currentUserId) {
        UserDeptRel relation = userDeptRelMapper.selectOne(
                new LambdaQueryWrapper<UserDeptRel>()
                        .eq(UserDeptRel::getUserId, currentUserId)
                        .eq(UserDeptRel::getIsPrimary, YesNoEnum.YES.getId())
                        .last("limit 1")
        );
        return relation != null ? relation.getDeptId() : null;
    }
}
