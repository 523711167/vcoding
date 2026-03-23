package com.yuyu.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuyu.workflow.entity.base.BaseCreateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务定义角色关联持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_biz_definition_role_rel")
public class BizDefinitionRoleRel extends BaseCreateEntity {

    /**
     * 业务定义ID。
     */
    private Long bizDefinitionId;

    /**
     * 角色ID。
     */
    private Long roleId;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer isDeleted;
}
