package com.yuyu.workflow.convert;

import com.yuyu.workflow.common.mapstruct.BaseMapper;
import com.yuyu.workflow.config.MapStructConfig;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.eto.biz.BizDefinitionCreateETO;
import com.yuyu.workflow.eto.biz.BizDefinitionUpdateETO;
import com.yuyu.workflow.vo.biz.BizDefinitionCurrentUserVO;
import com.yuyu.workflow.vo.biz.BizDefinitionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 业务定义对象转换组件。
 */
@Mapper(config = MapStructConfig.class)
public interface BizDefinitionStructMapper extends BaseMapper<BizDefinition, BizDefinitionVO> {

    /**
     * 将业务定义实体转换为返回对象。
     */
    @Override
    @Mapping(target = "statusMsg", ignore = true)
    @Mapping(target = "workflowDefinitionCode", ignore = true)
    @Mapping(target = "workflowDefinitionName", ignore = true)
    BizDefinitionVO toTarget(BizDefinition source);

    /**
     * 将业务定义实体转换为当前用户可见业务返回对象。
     */
    @Mapping(target = "statusMsg", ignore = true)
    BizDefinitionCurrentUserVO toCurrentUserTarget(BizDefinition source);

    /**
     * 将新增入参转换为业务定义实体。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    BizDefinition toEntity(BizDefinitionCreateETO eto);

    /**
     * 将修改入参更新为新的业务定义实体。
     */
    @Mapping(target = "id", source = "oldEntity.id")
    @Mapping(target = "bizCode", source = "oldEntity.bizCode")
    @Mapping(target = "createdBy", source = "oldEntity.createdBy")
    @Mapping(target = "createdAt", source = "oldEntity.createdAt")
    @Mapping(target = "updatedAt", source = "oldEntity.updatedAt")
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "bizName", source = "eto.bizName")
    @Mapping(target = "bizDesc", source = "eto.bizDesc")
    @Mapping(target = "workflowDefinitionId", source = "eto.workflowDefinitionId")
    @Mapping(target = "status", source = "eto.status")
    BizDefinition toUpdatedEntity(BizDefinitionUpdateETO eto, BizDefinition oldEntity);
}
