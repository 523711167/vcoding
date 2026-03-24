package com.yuyu.workflow.service;

import com.yuyu.workflow.entity.BizApply;

import java.util.List;

/**
 * 业务申请服务接口。
 */
public interface BizApplyService {

    /**
     * 按主键查询业务申请，不存在时抛出异常。
     */
    BizApply getByIdOrThrow(Long id);

    /**
     * 按流程实例主键集合查询业务申请。
     */
    List<BizApply> listByWorkflowInstanceIds(List<Long> workflowInstanceIdList);

    /**
     * 按主键集合删除业务申请。
     */
    void removeByIds(List<Long> idList);
}
