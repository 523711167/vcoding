package com.yuyu.workflow.convert;

import com.yuyu.workflow.common.mapstruct.BaseMapper;
import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.eto.dept.DeptCreateETO;
import com.yuyu.workflow.eto.dept.DeptUpdateETO;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.vo.dept.DeptTreeVO;
import com.yuyu.workflow.vo.dept.DeptVO;
import com.yuyu.workflow.vo.user.UserDeptVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface UserDeptStructMapper extends BaseMapper<UserDept, DeptVO> {

    /**
     * 将部门实体转换为部门视图。
     */
    @Override
    @Mapping(target = "orgTypeMsg", ignore = true)
    @Mapping(target = "statusMsg", ignore = true)
    DeptVO toTarget(UserDept source);

    /**
     * 将新增部门入参转换为部门实体。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(target = "leaderName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    UserDept toEntity(DeptCreateETO eto);

    /**
     * 将修改入参更新到部门实体。
     */
    @Mapping(target = "id", source = "oldEntity.id")
    @Mapping(target = "parentId", source = "oldEntity.parentId")
    @Mapping(target = "path", source = "oldEntity.path")
    @Mapping(target = "level", source = "oldEntity.level")
    @Mapping(target = "leaderName", source = "oldEntity.leaderName")
    @Mapping(target = "createdAt", source = "oldEntity.createdAt")
    @Mapping(target = "updatedAt", source = "oldEntity.updatedAt")
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "name", source = "eto.name")
    @Mapping(target = "code", source = "eto.code")
    @Mapping(target = "orgType", source = "oldEntity.orgType")
    @Mapping(target = "postType", source = "eto.postType")
    @Mapping(target = "sortOrder", source = "eto.sortOrder")
    @Mapping(target = "leaderId", source = "eto.leaderId")
    @Mapping(target = "status", source = "eto.status")
    UserDept toUpdatedEntity(DeptUpdateETO eto, UserDept oldEntity);

    /**
     * 将部门实体转换为树节点视图。
     */
    @Mapping(target = "orgTypeMsg", ignore = true)
    @Mapping(target = "statusMsg", ignore = true)
    @Mapping(target = "children", ignore = true)
    DeptTreeVO toTreeVO(UserDept entity);

    /**
     * 将部门实体转换为用户组织视图。
     */
    @Mapping(target = "orgTypeMsg", ignore = true)
    @Mapping(target = "isPrimary", ignore = true)
    @Mapping(target = "isPrimaryMsg", ignore = true)
    UserDeptVO toUserDeptVO(UserDept entity);
}
