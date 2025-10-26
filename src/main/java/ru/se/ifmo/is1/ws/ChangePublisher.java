package ru.se.ifmo.is1.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChangePublisher {
    private final SimpMessagingTemplate simp;

    public void broadcast(String entity, String action, Number id) {
        simp.convertAndSend("/topic/changes", new ChangeEvent(entity, action, id));
    }
}