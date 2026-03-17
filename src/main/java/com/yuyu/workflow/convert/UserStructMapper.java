package com.yuyu.workflow.convert;

import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.mapstruct.BaseMapper;
import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.eto.user.UserCreateETO;
import com.yuyu.workflow.eto.user.UserUpdateETO;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.vo.role.UserSimpleVO;
import com.yuyu.workflow.vo.user.RoleSimpleVO;
import com.yuyu.workflow.vo.user.UserDeptVO;
import com.yuyu.workflow.vo.user.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mapper(config = MapStructConfig.class)
public interface UserStructMapper extends BaseMapper<User, UserVO> {

    /**
     * 将新增用户入参转换为用户实体。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    User toEntity(UserCreateETO eto);

    /**
     * 将修改入参更新到用户实体。
     */
    @Mapping(target = "id", source = "oldEntity.id")
    @Mapping(target = "username", source = "oldEntity.username")
    @Mapping(target = "password", source = "oldEntity.password")
    @Mapping(target = "status", source = "oldEntity.status")
    @Mapping(target = "lastLoginAt", source = "oldEntity.lastLoginAt")
    @Mapping(target = "createdAt", source = "oldEntity.createdAt")
    @Mapping(target = "updatedAt", source = "oldEntity.updatedAt")
    @Mapping(target = "isDeleted", source = "oldEntity.isDeleted")
    @Mapping(target = "realName", source = "eto.realName")
    @Mapping(target = "email", source = "eto.email")
    @Mapping(target = "mobile", source = "eto.mobile")
    @Mapping(target = "avatar", source = "eto.avatar")
    User toUpdatedEntity(UserUpdateETO eto, User oldEntity);

    /**
     * 将用户实体转换为包含关联角色和组织的用户视图。
     */
    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "username", source = "entity.username")
    @Mapping(target = "realName", source = "entity.realName")
    @Mapping(target = "email", source = "entity.email")
    @Mapping(target = "mobile", source = "entity.mobile")
    @Mapping(target = "avatar", source = "entity.avatar")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "lastLoginAt", source = "entity.lastLoginAt")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    @Mapping(target = "updatedAt", source = "entity.updatedAt")
    @Mapping(target = "statusMsg", source = "statusMsg")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "depts", source = "depts")
    UserVO toUserVO(User entity, String statusMsg, List<RoleSimpleVO> roles, List<UserDeptVO> depts);

    /**
     * 将用户实体转换为包含状态文案、角色和组织信息的用户视图。
     */
    default UserVO toUserVO(User entity, List<RoleSimpleVO> roles, List<UserDeptVO> depts) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return toUserVO(
                entity,
                CommonStatusEnum.getMsgById(entity.getStatus()),
                Objects.isNull(roles) ? Collections.emptyList() : roles,
                Objects.isNull(depts) ? Collections.emptyList() : depts
        );
    }

    /**
     * 将用户实体列表转换为包含角色和组织信息的用户视图列表。
     */
    default List<UserVO> toUserVOList(List<User> userList,
                                      Map<Long, List<RoleSimpleVO>> roleMap,
                                      Map<Long, List<UserDeptVO>> deptMap) {
        if (Objects.isNull(userList) || userList.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<RoleSimpleVO>> safeRoleMap = Objects.isNull(roleMap) ? Collections.emptyMap() : roleMap;
        Map<Long, List<UserDeptVO>> safeDeptMap = Objects.isNull(deptMap) ? Collections.emptyMap() : deptMap;
        return userList.stream()
                .map(user -> toUserVO(
                        user,
                        safeRoleMap.getOrDefault(user.getId(), Collections.emptyList()),
                        safeDeptMap.getOrDefault(user.getId(), Collections.emptyList())
                ))
                .toList();
    }

    /**
     * 将用户实体转换为简版用户视图。
     */
    UserSimpleVO toUserSimpleVO(User entity);
}
