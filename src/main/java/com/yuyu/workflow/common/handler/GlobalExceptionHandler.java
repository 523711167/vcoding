package com.yuyu.workflow.common.handler;

import com.yuyu.workflow.common.Resp;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.exception.BizException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 全局异常捕获机制。
     *
     * <p>处理顺序遵循“越具体越靠前”的原则：</p>
     * <p>1. 业务异常：返回明确业务提示</p>
     * <p>2. 参数校验异常：返回统一参数错误信息</p>
     * <p>3. Web 请求异常：返回请求格式或方法错误</p>
     * <p>4. 兜底异常：记录完整堆栈，前端只返回系统异常</p>
     */
    @ExceptionHandler(BizException.class)
    public Resp<Void> handleBizException(BizException ex) {
        log.warn("biz exception, code={}, message={}", ex.getCode(), ex.getMessage());
        return Resp.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理请求体对象校验异常。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Resp<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return Resp.fail(RespCodeEnum.PARAM_ERROR.getId(),
                buildFieldErrorMessage(ex.getBindingResult().getFieldErrors()));
    }

    /**
     * 处理查询参数绑定异常。
     */
    @ExceptionHandler(BindException.class)
    public Resp<Void> handleBindException(BindException ex) {
        return Resp.fail(RespCodeEnum.PARAM_ERROR.getId(), buildFieldErrorMessage(ex.getFieldErrors()));
    }

    /**
     * 处理方法级约束校验异常。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Resp<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        return Resp.fail(RespCodeEnum.PARAM_ERROR.getId(), ex.getMessage());
    }

    /**
     * 处理登录认证异常。
     */
    @ExceptionHandler(AuthenticationException.class)
    public Resp<Void> handleAuthenticationException(AuthenticationException ex) {
        if (ex instanceof BadCredentialsException) {
            return Resp.fail(RespCodeEnum.UNAUTHORIZED.getId(), "用户名或密码错误");
        }
        if (ex instanceof DisabledException) {
            return Resp.fail(RespCodeEnum.UNAUTHORIZED.getId(), "用户已停用");
        }
        return Resp.fail(RespCodeEnum.UNAUTHORIZED.getId(), RespCodeEnum.UNAUTHORIZED.getName());
    }

    /**
     * 处理无权限访问异常。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Resp<Void> handleAccessDeniedException(AccessDeniedException ex) {
        return Resp.fail(RespCodeEnum.FORBIDDEN.getId(), RespCodeEnum.FORBIDDEN.getName());
    }

    /**
     * 处理缺少请求参数异常。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Resp<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return Resp.fail(RespCodeEnum.PARAM_ERROR.getId(), ex.getParameterName() + "不能为空");
    }

    /**
     * 处理请求参数类型不匹配异常。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Resp<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return Resp.fail(RespCodeEnum.PARAM_ERROR.getId(), ex.getName() + "参数类型错误");
    }

    /**
     * 处理请求体反序列化异常。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Resp<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return Resp.fail(RespCodeEnum.PARAM_ERROR.getId(), "请求体格式错误");
    }

    /**
     * 处理 HTTP 方法不支持异常。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Resp<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return Resp.fail(RespCodeEnum.PARAM_ERROR.getId(), "不支持的请求方式");
    }

    /**
     * 处理系统兜底异常。
     */
    @ExceptionHandler(Exception.class)
    public Resp<Void> handleException(Exception ex) {
        log.error("system exception", ex);
        return Resp.fail(RespCodeEnum.SYSTEM_ERROR.getId(), RespCodeEnum.SYSTEM_ERROR.getName());
    }

    /**
     * 提取首个字段校验错误信息。
     */
    private String buildFieldErrorMessage(Iterable<FieldError> fieldErrors) {
        String message = "";
        for (FieldError fieldError : fieldErrors) {
            message = fieldError.getField() + ":" + fieldError.getDefaultMessage();
            break;
        }
        return message;
    }
}
