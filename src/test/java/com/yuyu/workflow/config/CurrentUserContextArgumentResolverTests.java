package com.yuyu.workflow.config;

import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.eto.user.UserCreateETO;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.qto.menu.MenuTreeQTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 当前用户上下文参数注入测试。
 */
class CurrentUserContextArgumentResolverTests {

    private CurrentUserContextArgumentResolver resolver;
    private CurrentUserRequestBodyAdvice requestBodyAdvice;
    private UserDeptRelMapper userDeptRelMapper;

    @BeforeEach
    void setUp() {
        userDeptRelMapper = mock(UserDeptRelMapper.class);
        UserDeptRel relation = new UserDeptRel();
        relation.setDeptId(88L);
        when(userDeptRelMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(relation);

        CurrentUserContextFiller currentUserContextFiller = new CurrentUserContextFiller(userDeptRelMapper);
        resolver = new CurrentUserContextArgumentResolver(currentUserContextFiller);
        requestBodyAdvice = new CurrentUserRequestBodyAdvice(currentUserContextFiller);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 查询参数绑定完成后应自动补充当前操作人信息。
     */
    @Test
    void shouldBindQueryParamsAndFillCurrentUserContext() throws Exception {
        OAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(
                "admin",
                Map.of("user_id", 9L),
                java.util.List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sys/menu/tree");
        request.addParameter("name", "系统管理");
        request.addParameter("type", "MENU");
        request.addParameter("visible", "1");

        NativeWebRequest webRequest = new ServletWebRequest(request);
        ModelAndViewContainer mavContainer = new ModelAndViewContainer();
        WebDataBinderFactory binderFactory = new ServletRequestDataBinderFactory(null, null);
        Method method = TestController.class.getMethod("query", MenuTreeQTO.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        MenuTreeQTO qto = (MenuTreeQTO) resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        assertEquals("系统管理", qto.getName());
        assertEquals("MENU", qto.getType());
        assertEquals(1, qto.getVisible());
        assertEquals(9L, qto.getCurrentUserId());
        assertEquals("admin", qto.getCurrentUsername());
        assertEquals(88L, qto.getCurrentPrimaryDeptId());
    }

    /**
     * 请求体反序列化完成后应自动补充当前操作人信息。
     */
    @Test
    void shouldFillCurrentUserContextForRequestBody() throws Exception {
        OAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(
                "admin",
                Map.of("user_id", 9L),
                java.util.List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));

        Method method = TestController.class.getMethod("create", UserCreateETO.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        UserCreateETO eto = new UserCreateETO();
        eto.setUsername("tester");
        eto.setPassword("secret");
        eto.setRealName("测试用户");

        UserCreateETO result = (UserCreateETO) requestBodyAdvice.afterBodyRead(
                eto,
                null,
                parameter,
                parameter.getGenericParameterType(),
                MappingJackson2HttpMessageConverter.class
        );

        assertEquals(9L, result.getCurrentUserId());
        assertEquals("admin", result.getCurrentUsername());
        assertEquals(88L, result.getCurrentPrimaryDeptId());
    }

    /**
     * 仅对继承用户上下文基类的请求体参数启用注入。
     */
    @Test
    void shouldSupportRequestBodyParameter() throws Exception {
        Method method = TestController.class.getMethod("create", UserCreateETO.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        boolean supported = requestBodyAdvice.supports(
                parameter,
                parameter.getGenericParameterType(),
                MappingJackson2HttpMessageConverter.class
        );

        assertEquals(true, supported);
    }

    /**
     * 用于构造 MethodParameter 的测试控制器。
     */
    static class TestController {

        /**
         * 测试查询方法。
         */
        public void query(MenuTreeQTO qto) {
        }

        /**
         * 测试请求体方法。
         */
        public void create(@RequestBody UserCreateETO eto) {
        }
    }
}
