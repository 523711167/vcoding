package com.yuyu.workflow.entity.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 包含创建时间的持久化基类。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseCreateEntity extends BaseIdEntity {

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
