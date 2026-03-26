package com.yuyu.workflow.common;

import com.yuyu.workflow.common.enums.RespCodeEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.Map;

/**
 * 统一接口返回对象。
 *
 * <p>这里使用的是 Java 的档案类（record），它适合承载这种纯数据结构，核心特性如下：</p>
 * <p>1. 天然偏向不可变：字段在创建后不能再修改，适合作为返回值对象。</p>
 * <p>2. 自动生成构造方法、访问器、equals、hashCode、toString，代码量更少。</p>
 * <p>3. 语义上强调“只负责携带数据”，不适合放复杂业务状态和可变行为。</p>
 * <p>4. record 本身不能继承其他类，且不建议再额外声明可变实例字段。</p>
 *
 * <p>当前三个字段含义：</p>
 * <p>- code：业务状态码</p>
 * <p>- msg：响应说明</p>
 * <p>- data：具体业务数据</p>
 */
@Schema(description = "统一响应对象")
public record Resp<T>(
        @Schema(description = "业务状态码", example = "0") int code,
        @Schema(description = "响应消息", example = "success") String msg,
        @Schema(description = "业务数据") T data) {

    private static final Map<String, Object> EMPTY_DATA = Collections.emptyMap();

    /**
     * 构造成功响应。
     */
    @SuppressWarnings("unchecked")
    public static <T> Resp<T> success() {
        return new Resp<>(RespCodeEnum.SUCCESS.getId(), RespCodeEnum.SUCCESS.getName(), (T) EMPTY_DATA);
    }

    /**
     * 构造成功响应。
     */
    @SuppressWarnings("unchecked")
    public static <T> Resp<T> success(T data) {
        return data == null ? success() : new Resp<>(RespCodeEnum.SUCCESS.getId(), RespCodeEnum.SUCCESS.getName(), data);
    }

    /**
     * 构造失败响应。
     */
    @SuppressWarnings("unchecked")
    public static <T> Resp<T> fail(int code, String msg) {
        return new Resp<>(code, msg, (T) EMPTY_DATA);
    }
}
