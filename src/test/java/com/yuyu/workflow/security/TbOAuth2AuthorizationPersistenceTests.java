package com.yuyu.workflow.security;

import com.yuyu.workflow.security.password.PasswordLoginGrantAuthenticationToken;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * OAuth2 授权持久化测试。
 */
class JdbcOAuth2AuthorizationPersistenceTests {

    private JdbcTemplate jdbcTemplate;

    private RegisteredClient registeredClient;

    private JdbcOAuth2AuthorizationService authorizationService;

    private JdbcOAuth2AuthorizationConsentService authorizationConsentService;

    /**
     * 初始化测试依赖与表结构。
     */
    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:oauth2-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.execute("DROP TABLE IF EXISTS oauth2_authorization_consent");
        this.jdbcTemplate.execute("DROP TABLE IF EXISTS oauth2_authorization");
        this.jdbcTemplate.execute("""
                CREATE TABLE oauth2_authorization (
                  id VARCHAR(100) NOT NULL PRIMARY KEY,
                  registered_client_id VARCHAR(100) NOT NULL,
                  principal_name VARCHAR(200) NOT NULL,
                  authorization_grant_type VARCHAR(100) NOT NULL,
                  authorized_scopes VARCHAR(1000) DEFAULT NULL,
                  attributes BLOB DEFAULT NULL,
                  state VARCHAR(500) DEFAULT NULL,
                  authorization_code_value BLOB DEFAULT NULL,
                  authorization_code_issued_at TIMESTAMP NULL DEFAULT NULL,
                  authorization_code_expires_at TIMESTAMP NULL DEFAULT NULL,
                  authorization_code_metadata BLOB DEFAULT NULL,
                  access_token_value BLOB DEFAULT NULL,
                  access_token_issued_at TIMESTAMP NULL DEFAULT NULL,
                  access_token_expires_at TIMESTAMP NULL DEFAULT NULL,
                  access_token_metadata BLOB DEFAULT NULL,
                  access_token_type VARCHAR(100) DEFAULT NULL,
                  access_token_scopes VARCHAR(1000) DEFAULT NULL,
                  oidc_id_token_value BLOB DEFAULT NULL,
                  oidc_id_token_issued_at TIMESTAMP NULL DEFAULT NULL,
                  oidc_id_token_expires_at TIMESTAMP NULL DEFAULT NULL,
                  oidc_id_token_metadata BLOB DEFAULT NULL,
                  refresh_token_value BLOB DEFAULT NULL,
                  refresh_token_issued_at TIMESTAMP NULL DEFAULT NULL,
                  refresh_token_expires_at TIMESTAMP NULL DEFAULT NULL,
                  refresh_token_metadata BLOB DEFAULT NULL,
                  user_code_value BLOB DEFAULT NULL,
                  user_code_issued_at TIMESTAMP NULL DEFAULT NULL,
                  user_code_expires_at TIMESTAMP NULL DEFAULT NULL,
                  user_code_metadata BLOB DEFAULT NULL,
                  device_code_value BLOB DEFAULT NULL,
                  device_code_issued_at TIMESTAMP NULL DEFAULT NULL,
                  device_code_expires_at TIMESTAMP NULL DEFAULT NULL,
                  device_code_metadata BLOB DEFAULT NULL
                )
                """);
        this.jdbcTemplate.execute("""
                CREATE TABLE oauth2_authorization_consent (
                  registered_client_id VARCHAR(100) NOT NULL,
                  principal_name VARCHAR(200) NOT NULL,
                  authorities VARCHAR(1000) NOT NULL,
                  PRIMARY KEY (registered_client_id, principal_name)
                )
                """);
        this.registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("workflow-client")
                .clientSecret("{noop}workflow-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(PasswordLoginGrantAuthenticationToken.PASSWORD_LOGIN_GRANT_TYPE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .scope("api")
                .build();
        InMemoryRegisteredClientRepository registeredClientRepository =
                new InMemoryRegisteredClientRepository(this.registeredClient);
        this.authorizationService = new JdbcOAuth2AuthorizationService(this.jdbcTemplate, registeredClientRepository);
        this.authorizationConsentService =
                new JdbcOAuth2AuthorizationConsentService(this.jdbcTemplate, registeredClientRepository);
    }

    /**
     * 授权信息应能完成持久化、查询与删除。
     */
    @Test
    void shouldPersistAuthorizationIntoDatabase() {
        Instant issuedAt = Instant.now();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token-value",
                issuedAt,
                issuedAt.plusSeconds(7200),
                Set.of("api")
        );
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "refresh-token-value",
                issuedAt,
                issuedAt.plusSeconds(14400)
        );
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(this.registeredClient)
                .id("authorization-id")
                .principalName("admin")
                .authorizationGrantType(PasswordLoginGrantAuthenticationToken.PASSWORD_LOGIN_GRANT_TYPE)
                .authorizedScopes(Set.of("api"))
                .attribute("test_attr", "test-value")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        this.authorizationService.save(authorization);

        OAuth2Authorization savedById = this.authorizationService.findById("authorization-id");
        OAuth2Authorization savedByAccessToken =
                this.authorizationService.findByToken("access-token-value", OAuth2TokenType.ACCESS_TOKEN);
        OAuth2Authorization savedByRefreshToken =
                this.authorizationService.findByToken("refresh-token-value", OAuth2TokenType.REFRESH_TOKEN);

        assertNotNull(savedById);
        assertEquals("admin", savedById.getPrincipalName());
        assertNotNull(savedByAccessToken);
        assertNotNull(savedByRefreshToken);

        this.authorizationService.remove(savedById);

        assertNull(this.authorizationService.findById("authorization-id"));
    }

    /**
     * 授权确认信息应能完成持久化、查询与删除。
     */
    @Test
    void shouldPersistAuthorizationConsentIntoDatabase() {
        OAuth2AuthorizationConsent authorizationConsent = OAuth2AuthorizationConsent.withId(
                        this.registeredClient.getId(),
                        "admin"
                )
                .authority(new SimpleGrantedAuthority("SCOPE_api"))
                .build();

        this.authorizationConsentService.save(authorizationConsent);

        OAuth2AuthorizationConsent saved = this.authorizationConsentService.findById(
                this.registeredClient.getId(),
                "admin"
        );

        assertNotNull(saved);
        assertEquals(1, saved.getAuthorities().size());

        this.authorizationConsentService.remove(saved);

        assertNull(this.authorizationConsentService.findById(this.registeredClient.getId(), "admin"));
    }
}
