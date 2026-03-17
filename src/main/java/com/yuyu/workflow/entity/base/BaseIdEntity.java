package com.yuyu.workflow.entity.base;

import lombok.Data;

/**
 * 持久化对象主键基类。
 */
@Data
public abstract class BaseIdEntity {

    /**
     * 主键ID。
     */
    private Long id;
}
