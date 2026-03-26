package com.yuyu.workflow.common.enums;

/**
 * 菜单类型枚举。
 */
public enum MenuTypeEnum implements BaseEnum {

    DIRECTORY(1, "DIRECTORY", "目录"),
    MENU(2, "MENU", "菜单"),
    BUTTON(3, "BUTTON", "按钮");

    private final Integer id;
    private final String code;
    private final String msg;

    MenuTypeEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取菜单类型主键。
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * 获取菜单类型编码。
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取菜单类型说明。
     */
    @Override
    public String getName() {
        return msg;
    }

    /**
     * 根据 id 获取菜单类型说明。
     */
    public static String getMsgById(Integer id) {
        return EnumUtils.getMsgById(values(), id);
    }

    /**
     * 根据 code 获取菜单类型说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }
}
