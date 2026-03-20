package com.yuyu.workflow.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct 全局配置。
 *
 * <p>统一约束项目中的对象转换策略：</p>
 * <p>1. 生成的 Mapper 统一注册为 Spring Bean</p>
 * <p>2. 目标字段如未显式映射或忽略，则在编译期直接报错</p>
 * <p>3. null 属性更新时不覆盖目标对象已有值，适合 update 场景复用</p>
 * <p>4. 始终执行 null 检查，降低空指针风险</p>
 */
@MapperConfig(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface MapStructConfig {
}
