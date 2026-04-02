package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.enums.LoginResultEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.LoginLog;
import com.yuyu.workflow.mapper.LoginLogMapper;
import com.yuyu.workflow.qto.auth.LoginLogListQTO;
import com.yuyu.workflow.qto.auth.LoginLogPageQTO;
import com.yuyu.workflow.service.LoginLogService;
import com.yuyu.workflow.struct.LoginLogStructMapper;
import com.yuyu.workflow.vo.auth.LoginLogVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 登录日志服务实现。
 */
@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    private static final int USERNAME_MAX_LENGTH = 64;
    private static final int FAIL_REASON_MAX_LENGTH = 255;
    private static final int CLIENT_IP_MAX_LENGTH = 64;
    private static final int USER_AGENT_MAX_LENGTH = 512;

    private final LoginLogStructMapper loginLogStructMapper;

    /**
     * 注入登录日志服务依赖。
     */
    public LoginLogServiceImpl(LoginLogMapper loginLogMapper,
                               LoginLogStructMapper loginLogStructMapper) {
        this.baseMapper = loginLogMapper;
        this.loginLogStructMapper = loginLogStructMapper;
    }

    @Override
    public void recordSuccess(Long userId, String username, String clientIp, String userAgent) {
        persist(userId, username, LoginResultEnum.SUCCESS.getCode(), null, clientIp, userAgent);
    }

    @Override
    public void recordFailure(String username, String failReason, String clientIp, String userAgent) {
        persist(null, username, LoginResultEnum.FAIL.getCode(), failReason, clientIp, userAgent);
    }

    @Override
    public List<LoginLogVO> list(LoginLogListQTO qto) {
        List<LoginLog> entityList = baseMapper.selectList(buildQuery(
                qto.getUsername(),
                qto.getResult(),
                qto.getLoginAtStart(),
                qto.getLoginAtEnd()
        ));
        if (CollectionUtils.isEmpty(entityList)) {
            return Collections.emptyList();
        }
        return entityList.stream().map(loginLogStructMapper::toVO).toList();
    }

    @Override
    public PageVo<LoginLogVO> page(LoginLogPageQTO qto) {
        IPage<LoginLog> page = baseMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(qto.getPageNum(), qto.getPageSize()),
                buildQuery(
                        qto.getUsername(),
                        qto.getResult(),
                        qto.getLoginAtStart(),
                        qto.getLoginAtEnd()
                )
        );
        List<LoginLogVO> voList = CollectionUtils.isEmpty(page.getRecords())
                ? Collections.emptyList()
                : page.getRecords().stream().map(loginLogStructMapper::toVO).toList();
        return PageVo.of(page.getCurrent(), page.getSize(), page.getTotal(), voList);
    }

    @Override
    public LoginLogVO detail(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        LoginLog entity = baseMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BizException(RespCodeEnum.BIZ_ERROR.getId(), "登录日志不存在");
        }
        return loginLogStructMapper.toVO(entity);
    }

    /**
     * 按统一口径落库登录日志。
     */
    private void persist(Long userId,
                         String username,
                         String result,
                         String failReason,
                         String clientIp,
                         String userAgent) {
        LoginLog entity = loginLogStructMapper.toEntity(
                userId,
                trimToNull(username, USERNAME_MAX_LENGTH),
                result,
                trimToNull(failReason, FAIL_REASON_MAX_LENGTH),
                trimToNull(clientIp, CLIENT_IP_MAX_LENGTH),
                trimToNull(userAgent, USER_AGENT_MAX_LENGTH),
                LocalDateTime.now()
        );
        baseMapper.insert(entity);
    }

    /**
     * 构造登录日志查询条件。
     */
    private LambdaQueryWrapper<LoginLog> buildQuery(String username,
                                                    String result,
                                                    LocalDateTime loginAtStart,
                                                    LocalDateTime loginAtEnd) {
        return new LambdaQueryWrapper<LoginLog>()
                .like(StringUtils.hasText(username), LoginLog::getUsername, username)
                .eq(StringUtils.hasText(result), LoginLog::getResult, result)
                .ge(Objects.nonNull(loginAtStart), LoginLog::getLoginAt, loginAtStart)
                .le(Objects.nonNull(loginAtEnd), LoginLog::getLoginAt, loginAtEnd)
                .orderByDesc(LoginLog::getLoginAt, LoginLog::getId);
    }

    /**
     * 统一裁剪并清洗文本字段。
     */
    private String trimToNull(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
