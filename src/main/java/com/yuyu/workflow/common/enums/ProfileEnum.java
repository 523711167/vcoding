package com.yuyu.workflow.common.enums;

/**
 * 运行环境枚举。
 *
 * <p>约束项目内涉及环境标识的常量，避免直接散落使用 "dev"、"prod" 这类字符串。</p>
 */
public enum ProfileEnum implements BaseEnum {

    DEV(1, "dev", "开发环境"),
    PROD(2, "prod", "生产环境");

    private final Integer id;
    private final String code;
    private final String msg;

    ProfileEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取环境枚举主键。
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * 获取环境编码。
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取环境说明。
     */
    @Override
    public String getName() {
        return msg;
    }

    /**
     * 根据 id 获取环境说明。
     */
    public static String getMsgById(Integer id) {
        return EnumUtils.getMsgById(values(), id);
    }

    /**
     * 根据 code 获取环境说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }
}
