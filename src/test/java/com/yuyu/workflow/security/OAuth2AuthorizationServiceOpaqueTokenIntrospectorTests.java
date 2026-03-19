package com.yuyu.workflow.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Opaque token 内省测试。
 */
@ExtendWith(MockitoExtension.class)
class OAuth2AuthorizationServiceOpaqueTokenIntrospectorTests {

    @Mock
    private OAuth2AuthorizationService authorizationService;

    @Mock
    private AuthUserDetailsService authUserDetailsService;

    @InjectMocks
    private OAuth2AuthorizationServiceOpaqueTokenIntrospector opaqueTokenIntrospector;

    /**
     * 历史 token 关联的客户端不存在时，应按无效 token 处理而不是抛出系统异常。
     */
    @Test
    void shouldTreatMissingRegisteredClientAsInvalidToken() {
        when(authorizationService.findByToken("stale-token", OAuth2TokenType.ACCESS_TOKEN))
                .thenThrow(new DataRetrievalFailureException("RegisteredClient not found"));

        assertThrows(BadOpaqueTokenException.class, () -> opaqueTokenIntrospector.introspect("stale-token"));
    }
}
