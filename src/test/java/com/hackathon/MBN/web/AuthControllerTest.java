package com.hackathon.MBN.web;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.User;
import com.hackathon.MBN.repository.UserRepository;

class AuthControllerTest {

    @Test
    void createDeviceTokenIssuesToken() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthController controller = new AuthController(userRepository);
        Map<String, Object> response = controller.createDeviceToken();

        assertEquals(32, ((String) response.get("token")).length());
    }

    @Test
    void fallsBackToEnglishForUnsupportedLanguage() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User user = User.builder().id(1L).deviceToken("token").preferredLang("en").build();

        AuthController controller = new AuthController(userRepository);
        Map<String, Object> response = controller.updatePreferences(
                user, new AuthController.PreferencesRequest("fr", null, null, Set.of("KPOP")));

        assertEquals("en", response.get("preferredLang"));
    }

    @Test
    void getPreferencesDoesNotThrowWhenPinTypeFilterIsNull() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = User.builder().id(1L).deviceToken("token").preferredLang("ko").build();

        AuthController controller = new AuthController(userRepository);
        Map<String, Object> response = controller.getPreferences(user);

        assertEquals("ko", response.get("preferredLang"));
        assertNull(response.get("pinTypeFilter"));
    }
}
