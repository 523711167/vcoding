package com.yuyu.workflow.service;

import java.util.List;

/**
 * 业务定义角色关联服务接口。
 */
public interface BizDefinitionRoleRelService {

    /**
     * 按业务定义主键全量替换绑定角色。
     */
    void replaceRoles(Long bizDefinitionId, List<Long> roleIds);

    /**
     * 查询业务定义对应的已绑定角色主键集合。
     */
    List<Long> listRoleIdsByBizDefinitionId(Long bizDefinitionId);

    /**
     * 按业务定义主键集合删除角色关联配置。
     */
    void removeByBizDefinitionIds(List<Long> bizDefinitionIdList);
}
