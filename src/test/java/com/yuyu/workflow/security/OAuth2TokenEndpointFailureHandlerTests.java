package com.yuyu.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OAuth2 token 端点失败响应测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OAuth2TokenEndpointFailureHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 初始化登录测试所需数据表和用户数据。
     */
    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS tb_user_role_menu");
        jdbcTemplate.execute("DROP TABLE IF EXISTS tb_user_role_rel");
        jdbcTemplate.execute("DROP TABLE IF EXISTS tb_sys_menu");
        jdbcTemplate.execute("DROP TABLE IF EXISTS tb_user_role");
        jdbcTemplate.execute("DROP TABLE IF EXISTS tb_login_log");
        jdbcTemplate.execute("DROP TABLE IF EXISTS tb_user");

        jdbcTemplate.execute("""
                CREATE TABLE tb_user (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(64),
                    password VARCHAR(255),
                    real_name VARCHAR(64),
                    avatar VARCHAR(255),
                    status INT,
                    is_deleted INT,
                    created_at TIMESTAMP NULL,
                    updated_at TIMESTAMP NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE tb_user_role (
                    id BIGINT PRIMARY KEY,
                    code VARCHAR(64),
                    status INT,
                    is_deleted INT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE tb_user_role_rel (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT,
                    role_id BIGINT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE tb_sys_menu (
                    id BIGINT PRIMARY KEY,
                    permission VARCHAR(128),
                    status INT,
                    is_deleted INT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE tb_user_role_menu (
                    id BIGINT PRIMARY KEY,
                    role_id BIGINT,
                    menu_id BIGINT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE tb_login_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT,
                    username VARCHAR(64),
                    result VARCHAR(16),
                    fail_reason VARCHAR(255),
                    client_ip VARCHAR(64),
                    user_agent VARCHAR(512),
                    login_at TIMESTAMP NULL,
                    created_at TIMESTAMP NULL,
                    updated_at TIMESTAMP NULL,
                    is_deleted INT DEFAULT 0
                )
                """);

        jdbcTemplate.update(
                "INSERT INTO tb_user (id, username, password, real_name, avatar, status, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L,
                "admin",
                passwordEncoder.encode("admin123"),
                "系统管理员",
                "",
                1,
                0
        );
    }

    /**
     * 用户名不存在时应返回统一 JSON 响应。
     */
    @Test
    void shouldReturnJsonWhenUsernameNotFound() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password_login")
                        .param("client_id", "workflow-client")
                        .param("client_secret", "workflow-client-secret")
                        .param("username", "not_exists_user")
                        .param("password", "wrong_pass")
                        .param("scope", "api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data").isEmpty());
        assertEquals(1, countByResult("FAIL"));
        assertEquals("not_exists_user", latestUsername());
        assertEquals("用户名或密码错误", latestFailReason());
    }

    /**
     * 密码错误时应返回统一 JSON 响应。
     */
    @Test
    void shouldReturnJsonWhenPasswordInvalid() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password_login")
                        .param("client_id", "workflow-client")
                        .param("client_secret", "workflow-client-secret")
                        .param("username", "admin")
                        .param("password", "wrong_pass")
                        .param("scope", "api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data").isEmpty());
        assertEquals(1, countByResult("FAIL"));
        assertEquals("admin", latestUsername());
        assertEquals("用户名或密码错误", latestFailReason());
    }

    /**
     * 非 password_login 模式失败时不记录登录失败日志。
     */
    @Test
    void shouldNotRecordLoginFailWhenGrantTypeIsNotPasswordLogin() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", "workflow-client")
                        .param("client_secret", "wrong-secret")
                        .param("scope", "api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        assertEquals(0, countByResult("FAIL"));
    }

    /**
     * 统计指定结果日志数量。
     */
    private int countByResult(String result) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM tb_login_log WHERE result = ? AND is_deleted = 0",
                Integer.class,
                result
        );
        return count == null ? 0 : count;
    }

    /**
     * 查询最近一条登录日志用户名。
     */
    private String latestUsername() {
        return jdbcTemplate.queryForObject(
                "SELECT username FROM tb_login_log ORDER BY id DESC LIMIT 1",
                String.class
        );
    }

    /**
     * 查询最近一条登录日志失败原因。
     */
    private String latestFailReason() {
        return jdbcTemplate.queryForObject(
                "SELECT fail_reason FROM tb_login_log ORDER BY id DESC LIMIT 1",
                String.class
        );
    }
}
