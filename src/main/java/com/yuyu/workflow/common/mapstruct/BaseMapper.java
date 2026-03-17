package com.yuyu.workflow.common.mapstruct;

import java.util.List;

/**
 * MapStruct 基础转换接口。
 *
 * <p>后续具体业务 Mapper 可直接继承本接口，统一提供常见的单对象、列表转换能力。</p>
 */
public interface BaseMapper<S, T> {

    /**
     * 将源对象转换为目标对象。
     */
    T toTarget(S source);

    /**
     * 将源对象列表转换为目标对象列表。
     */
    List<T> toTargetList(List<S> sourceList);

}
