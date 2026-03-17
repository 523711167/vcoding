package com.yuyu.workflow.common.enums;

public enum RespCodeEnum implements BaseEnum {

    SUCCESS(0, "SUCCESS", "success"),
    BIZ_ERROR(1, "BIZ_ERROR", "业务异常"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "未登录或登录已失效"),
    PARAM_ERROR(400, "PARAM_ERROR", "参数错误"),
    FORBIDDEN(403, "FORBIDDEN", "无权限访问"),
    SYSTEM_ERROR(500, "SYSTEM_ERROR", "系统异常");

    private final Integer id;
    private final String code;
    private final String msg;

    RespCodeEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取响应码主键。
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * 获取响应码编码。
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取响应码说明。
     */
    @Override
    public String getMsg() {
        return msg;
    }

    /**
     * 根据 id 获取响应码说明。
     */
    public static String getMsgById(Integer id) {
        return EnumUtils.getMsgById(values(), id);
    }

    /**
     * 根据 code 获取响应码说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }
}
