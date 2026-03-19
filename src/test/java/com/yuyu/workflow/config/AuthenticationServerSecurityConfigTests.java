package com.yuyu.workflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 认证服务端客户端配置测试。
 */
class AuthenticationServerSecurityConfigTests {

    /**
     * RegisteredClient 内部主键应保持稳定，避免服务重启后旧 token 无法内省。
     */
    @Test
    void shouldUseStableRegisteredClientInternalId() {
        SecurityTokenProperties properties = new SecurityTokenProperties();
        properties.setClientId("workflow-client");
        properties.setClientSecret("workflow-client-secret");
        properties.setClientName("Workflow Client");
        properties.setClientUuid("f8f6c594-ce98-4eeb-b0a1-13222623e596");

        AuthenticationServerSecurityConfig config = new AuthenticationServerSecurityConfig(properties);
        RegisteredClientRepository repository = config.registeredClientRepository(
                PasswordEncoderFactories.createDelegatingPasswordEncoder()
        );

        RegisteredClient registeredClient = repository.findByClientId("workflow-client");

        assertNotNull(registeredClient);
        assertEquals("f8f6c594-ce98-4eeb-b0a1-13222623e596", registeredClient.getId());
    }
}
