package com.hackathon.MBN.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.User;
import com.hackathon.MBN.repository.UserRepository;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class CurrentUserArgumentResolverTest {

    @Test
    void resolvesUserFromBearerToken() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        User user = User.builder().id(1L).deviceToken("abc123").build();
        when(userRepository.findByDeviceToken("abc123")).thenReturn(Optional.of(user));

        CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver(userRepository);
        Method method = StubController.class.getDeclaredMethod("handle", User.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc123");
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Object resolved = resolver.resolveArgument(parameter, null, webRequest, null);

        assertEquals(user, resolved);
    }

    @Test
    void throwsUnauthorizedWhenHeaderIsMissing() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver(userRepository);
        Method method = StubController.class.getDeclaredMethod("handle", User.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletWebRequest webRequest = new ServletWebRequest(request);

        assertThrows(ApiException.class, () -> resolver.resolveArgument(parameter, null, webRequest, null));
    }

    private static class StubController {
        public void handle(@CurrentUser User user) {
        }
    }
}
