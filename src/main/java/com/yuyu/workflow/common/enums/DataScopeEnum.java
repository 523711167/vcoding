package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

public enum DataScopeEnum implements BaseEnum {

    ALL(1, "ALL", "全部数据"),
    CUSTOM_DEPT(2, "CUSTOM_DEPT", "自定义部门"),
    CURRENT_AND_CHILD_DEPT(3, "CURRENT_AND_CHILD_DEPT", "本部门及子部门"),
    CURRENT_DEPT(4, "CURRENT_DEPT", "仅本部门"),
    SELF(5, "SELF", "仅本人数据");

    private final Integer id;
    private final String code;
    private final String msg;

    DataScopeEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取数据权限主键。
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * 获取数据权限编码。
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取数据权限说明。
     */
    @Override
    public String getName() {
        return msg;
    }

    /**
     * 根据 id 获取数据权限说明。
     */
    public static String getMsgById(Integer id) {
        return EnumUtils.getMsgById(values(), id);
    }

    /**
     * 根据 code 判断数据权限是否存在。
     */
    public static boolean containsCode(String code) {
        return Arrays.stream(values()).anyMatch(item -> Objects.equals(item.getCode(), code));
    }

    /**
     * 根据 code 获取数据权限说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }
}
