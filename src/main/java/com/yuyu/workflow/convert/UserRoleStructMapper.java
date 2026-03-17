package com.yuyu.workflow.convert;

import com.yuyu.workflow.common.mapstruct.BaseMapper;
import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.eto.role.RoleCreateETO;
import com.yuyu.workflow.eto.role.RoleUpdateETO;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.vo.role.RoleVO;
import com.yuyu.workflow.vo.user.RoleSimpleVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface UserRoleStructMapper extends BaseMapper<UserRole, RoleVO> {

    /**
     * 将新增角色入参转换为角色实体。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dataScope", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    UserRole toEntity(RoleCreateETO eto);

    /**
     * 将修改入参更新到角色实体。
     */
    @Mapping(target = "id", source = "oldEntity.id")
    @Mapping(target = "code", source = "oldEntity.code")
    @Mapping(target = "status", source = "oldEntity.status")
    @Mapping(target = "dataScope", source = "oldEntity.dataScope")
    @Mapping(target = "createdAt", source = "oldEntity.createdAt")
    @Mapping(target = "updatedAt", source = "oldEntity.updatedAt")
    @Mapping(target = "isDeleted", source = "oldEntity.isDeleted")
    @Mapping(target = "name", source = "eto.name")
    @Mapping(target = "description", source = "eto.description")
    @Mapping(target = "sortOrder", source = "eto.sortOrder")
    UserRole toUpdatedEntity(RoleUpdateETO eto, UserRole oldEntity);

    /**
     * 将角色实体转换为简版角色视图。
     */
    RoleSimpleVO toRoleSimpleVO(UserRole entity);
}
