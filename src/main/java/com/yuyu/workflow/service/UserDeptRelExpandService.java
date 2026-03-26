package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.UserDeptRelExpand;

import java.util.List;

public interface UserDeptRelExpandService extends IService<UserDeptRelExpand> {

    /**
     * 按用户集合全量重建展开关系。
     */
    void rebuildByUserIds(List<Long> userIds);

    /**
     * 按组织路径集合重建受影响用户的展开关系。
     */
    void rebuildByDeptPaths(List<String> deptPaths);
}
