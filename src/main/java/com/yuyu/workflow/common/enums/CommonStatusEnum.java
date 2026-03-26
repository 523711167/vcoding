package com.yuyu.workflow.common.enums;

public enum CommonStatusEnum implements BaseEnum {

    DISABLED(0, "DISABLED", "停用"),
    ENABLED(1, "ENABLED", "正常");

    private final Integer id;
    private final String code;
    private final String msg;

    CommonStatusEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取状态主键。
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * 获取状态编码。
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取状态说明。
     */
    @Override
    public String getName() {
        return msg;
    }

    /**
     * 根据 id 获取状态说明。
     */
    public static String getMsgById(Integer id) {
        return EnumUtils.getMsgById(values(), id);
    }

    /**
     * 根据 code 获取状态说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }
}
