package com.yuyu.workflow.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角色编码枚举测试。
 */
class RoleCodeEnumTests {

    /**
     * 应返回 ADMIN 对应文案。
     */
    @Test
    void shouldReturnAdminMessageByCode() {
        assertEquals("系统管理员", RoleCodeEnum.getMsgByCode("ADMIN"));
    }

    /**
     * 应正确判断角色编码是否存在。
     */
    @Test
    void shouldCheckRoleCodeExists() {
        assertTrue(RoleCodeEnum.containsCode("ADMIN"));
        assertFalse(RoleCodeEnum.containsCode("UNKNOWN"));
    }
}
