package com.yuyu.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyu.workflow.entity.BizApply;

import java.util.List;

/**
 * 业务申请服务接口。
 */
public interface BizApplyService extends IService<BizApply> {

    /**
     * 按主键查询业务申请，不存在时抛出异常。
     */
    BizApply getByIdOrThrow(Long id);

    /**
     * 按流程实例主键集合查询业务申请。
     */
    List<BizApply> listByWorkflowInstanceIds(List<Long> workflowInstanceIdList);

}
