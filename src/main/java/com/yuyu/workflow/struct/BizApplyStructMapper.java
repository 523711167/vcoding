package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.eto.biz.BizApplySaveAndSubmitETO;
import com.yuyu.workflow.eto.workflow.WorkflowBizSubmitETO;
import com.yuyu.workflow.vo.biz.BizApplyDraftVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 业务申请对象转换。
 */
@Mapper(config = MapStructConfig.class)
public interface BizApplyStructMapper {

    /**
     * 保存并提交参数转换为流程提交参数。
     */
    @Mapping(target = "bizApplyId", ignore = true)
    WorkflowBizSubmitETO toWorkflowBizSubmitETO(BizApplySaveAndSubmitETO eto);

    /**
     * 业务申请转换为草稿返回对象。
     */
    BizApplyDraftVO toBizApplyDraftVO(BizApply bizApply);
}
