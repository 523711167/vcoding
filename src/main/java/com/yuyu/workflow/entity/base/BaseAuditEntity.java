package com.yuyu.workflow.entity.base;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 包含审计字段和逻辑删除标记的持久化基类。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseAuditEntity extends BaseCreateEntity {

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer isDeleted;
}
