package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.UserRoleDeptExpand;

import java.util.List;

public interface UserRoleDeptExpandService extends IService<UserRoleDeptExpand> {

    /**
     * 按角色集合全量重建展开关系。
     */
    void rebuildByRoleIds(List<Long> roleIds);

    /**
     * 按组织路径集合重建受影响角色的展开关系。
     */
    void rebuildByDeptPaths(List<String> deptPaths);
}
