package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.User;
import com.hackathon.MBN.repository.UserRepository;

class UserPreferenceControllerTest {

    @Test
    void managesLanguageAndCategories() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = User.builder().id(1L).deviceToken("token").preferredLang("en").build();
        UserPreferenceController controller = new UserPreferenceController(userRepository);

        Map<String, Object> language = controller.updateLanguage(user, new UserPreferenceController.LanguageRequest("ko"));
        Map<String, Object> categories = controller.updateCategories(user, new UserPreferenceController.CategoryRequest(List.of("KPOP", "DRAMA")));

        assertEquals("ko", language.get("language"));
        assertEquals(List.of("DRAMA", "KPOP"), categories.get("categories"));
    }
}
