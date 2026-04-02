package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.LoginLog;
import com.yuyu.workflow.mapper.LoginLogMapper;
import com.yuyu.workflow.qto.auth.LoginLogListQTO;
import com.yuyu.workflow.qto.auth.LoginLogPageQTO;
import com.yuyu.workflow.struct.LoginLogStructMapper;
import com.yuyu.workflow.vo.auth.LoginLogVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 登录日志服务测试。
 */
@ExtendWith(MockitoExtension.class)
class LoginLogServiceImplTests {

    @Mock
    private LoginLogMapper loginLogMapper;

    @Mock
    private LoginLogStructMapper loginLogStructMapper;

    @InjectMocks
    private LoginLogServiceImpl loginLogService;

    /**
     * 列表查询应返回结构化结果。
     */
    @Test
    void shouldReturnLoginLogList() {
        LoginLog entity = buildEntity(1L, "admin", "SUCCESS");
        LoginLogVO vo = buildVO(1L, "admin", "SUCCESS", "登录成功");
        when(loginLogMapper.selectList(any())).thenReturn(List.of(entity));
        when(loginLogStructMapper.toVO(entity)).thenReturn(vo);

        LoginLogListQTO qto = new LoginLogListQTO();
        qto.setUsername("admin");
        qto.setResult("SUCCESS");
        List<LoginLogVO> result = loginLogService.list(qto);

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
        assertEquals("SUCCESS", result.get(0).getResult());
    }

    /**
     * 分页查询应返回分页结构。
     */
    @Test
    void shouldReturnLoginLogPage() {
        LoginLog entity = buildEntity(2L, "tester", "FAIL");
        LoginLogVO vo = buildVO(2L, "tester", "FAIL", "登录失败");
        Page<LoginLog> page = new Page<>(1L, 10L);
        page.setTotal(1L);
        page.setRecords(List.of(entity));
        when(loginLogMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(loginLogStructMapper.toVO(entity)).thenReturn(vo);

        LoginLogPageQTO qto = new LoginLogPageQTO();
        qto.setPageNum(1L);
        qto.setPageSize(10L);
        var pageVo = loginLogService.page(qto);

        assertEquals(1L, pageVo.total());
        assertEquals(1, pageVo.records().size());
        assertEquals("FAIL", pageVo.records().get(0).getResult());
    }

    /**
     * 详情查询不存在时应抛业务异常。
     */
    @Test
    void shouldThrowWhenDetailNotFound() {
        when(loginLogMapper.selectById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> loginLogService.detail(99L));
    }

    /**
     * 详情查询存在时应返回视图对象。
     */
    @Test
    void shouldReturnDetail() {
        LoginLog entity = buildEntity(3L, "ops", "SUCCESS");
        LoginLogVO vo = buildVO(3L, "ops", "SUCCESS", "登录成功");
        when(loginLogMapper.selectById(3L)).thenReturn(entity);
        when(loginLogStructMapper.toVO(entity)).thenReturn(vo);

        LoginLogVO result = loginLogService.detail(3L);

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("ops", result.getUsername());
    }

    private LoginLog buildEntity(Long id, String username, String result) {
        LoginLog entity = new LoginLog();
        entity.setId(id);
        entity.setUserId(id);
        entity.setUsername(username);
        entity.setResult(result);
        entity.setLoginAt(LocalDateTime.now());
        return entity;
    }

    private LoginLogVO buildVO(Long id, String username, String result, String resultMsg) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(id);
        vo.setUserId(id);
        vo.setUsername(username);
        vo.setResult(result);
        vo.setResultMsg(resultMsg);
        vo.setLoginAt(LocalDateTime.now());
        return vo;
    }
}
