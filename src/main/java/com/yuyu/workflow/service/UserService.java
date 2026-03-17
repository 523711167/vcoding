package com.yuyu.workflow.service;

import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.eto.user.UserCreateETO;
import com.yuyu.workflow.eto.user.UserDeptsUpdateETO;
import com.yuyu.workflow.eto.user.UserPasswordResetETO;
import com.yuyu.workflow.eto.user.UserRolesUpdateETO;
import com.yuyu.workflow.eto.user.UserStatusUpdateETO;
import com.yuyu.workflow.eto.user.UserUpdateETO;
import com.yuyu.workflow.qto.user.UserListQTO;
import com.yuyu.workflow.qto.user.UserPageQTO;
import com.yuyu.workflow.vo.user.RoleSimpleVO;
import com.yuyu.workflow.vo.user.UserDeptVO;
import com.yuyu.workflow.vo.user.UserVO;

import java.util.List;

public interface UserService {

    /**
     * 创建用户。
     */
    UserVO create(UserCreateETO eto);

    /**
     * 更新用户基本信息。
     */
    UserVO update(UserUpdateETO eto);

    /**
     * 按主键集合批量删除用户，单个删除视为批量删除的特例。
     */
    void delete(List<Long> idList);

    /**
     * 查询用户列表。
     */
    List<UserVO> list(UserListQTO qto);

    /**
     * 分页查询用户列表。
     */
    PageVo<UserVO> page(UserPageQTO qto);

    /**
     * 查询用户详情。
     */
    UserVO detail(Long id);

    /**
     * 重置用户密码。
     */
    void resetPassword(UserPasswordResetETO eto);

    /**
     * 更新用户启停状态。
     */
    void updateStatus(UserStatusUpdateETO eto);

    /**
     * 全量更新用户角色。
     */
    void updateRoles(UserRolesUpdateETO eto);

    /**
     * 查询用户已关联角色。
     */
    List<RoleSimpleVO> getRoles(Long userId);

    /**
     * 全量更新用户组织。
     */
    void updateDepts(UserDeptsUpdateETO eto);

    /**
     * 查询用户已关联组织。
     */
    List<UserDeptVO> getDepts(Long userId);
}
