package com.yuyu.workflow.common.exception;

import com.yuyu.workflow.common.enums.RespCodeEnum;

public class BizException extends RuntimeException {

    private final int code;

    /**
     * 使用默认业务异常码创建异常。
     */
    public BizException(String message) {
        this(RespCodeEnum.BIZ_ERROR.getId(), message);
    }

    /**
     * 使用指定异常码创建异常。
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取业务异常码。
     */
    public int getCode() {
        return code;
    }
}
