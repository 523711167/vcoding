package com.yuyu.workflow.common.enums;

public enum YesNoEnum implements BaseEnum {

    NO(0, "NO", "否"),
    YES(1, "YES", "是");

    private final Integer id;
    private final String code;
    private final String msg;

    YesNoEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取是/否主键。
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * 获取是/否编码。
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取是/否说明。
     */
    @Override
    public String getName() {
        return msg;
    }

    /**
     * 根据 id 获取是/否说明。
     */
    public static String getMsgById(Integer id) {
        return EnumUtils.getMsgById(values(), id);
    }

    /**
     * 根据 code 获取是/否说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }

    public static boolean isNo(String code) {
        return NO.getCode().equals(code);
    }

    public static boolean isYes(String code) {
        return YES.getCode().equals(code);
    }

    public static boolean isNo(Integer id) {
        return NO.getId().equals(id);
    }

    public static boolean isYes(Integer id) {
        return YES.getId().equals(id);
    }
}
