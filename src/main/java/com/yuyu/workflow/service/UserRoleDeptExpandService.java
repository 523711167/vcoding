package com.yuyu.workflow.service;

import java.util.List;

public interface UserRoleDeptExpandService {

    /**
     * 按角色集合全量重建展开关系。
     */
    void rebuildByRoleIds(List<Long> roleIds);

    /**
     * 按组织路径集合重建受影响角色的展开关系。
     */
    void rebuildByDeptPaths(List<String> deptPaths);
}
