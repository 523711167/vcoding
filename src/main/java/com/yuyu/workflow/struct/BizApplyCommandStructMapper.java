package com.yuyu.workflow.struct;

import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.eto.biz.BizApplySaveAndSubmitETO;
import com.yuyu.workflow.eto.workflow.WorkflowBizSubmitETO;
import com.yuyu.workflow.vo.biz.BizApplyDraftVO;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface BizApplyCommandStructMapper {

    @Mapping(target = "bizApplyId", ignore = true)
    WorkflowBizSubmitETO toWorkflowBizSubmitETO(BizApplySaveAndSubmitETO eto);

    BizApplyDraftVO toBizApplyDraftVO(BizApply bizApply);
}
