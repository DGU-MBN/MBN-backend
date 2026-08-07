package com.hackathon.MBN.web;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hackathon.MBN.domain.Short;
import com.hackathon.MBN.domain.User;
import com.hackathon.MBN.repository.InteractionRepository;
import com.hackathon.MBN.repository.ShortRepository;

class InteractionControllerTest {

    @Test
    void createsInteractionAndReturnsAccepted() {
        ShortRepository shortRepository = mock(ShortRepository.class);
        InteractionRepository interactionRepository = mock(InteractionRepository.class);
        User user = User.builder().id(1L).deviceToken("token").build();
        Short shortVideo = Short.builder().id(7L).title("Short").lang("en").build();

        when(shortRepository.findById(7L)).thenReturn(Optional.of(shortVideo));
        when(interactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InteractionController controller = new InteractionController(shortRepository, interactionRepository);
        ResponseEntity<Map<String, Object>> response = controller.createInteraction(
                7L,
                user,
                new InteractionController.InteractionRequest("LIKE", 0.8, "KR"));

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(interactionRepository).save(any());
    }
}
