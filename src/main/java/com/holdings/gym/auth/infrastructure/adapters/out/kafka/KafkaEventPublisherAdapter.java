package com.holdings.gym.auth.infrastructure.adapters.out.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holdings.gym.auth.domain.ports.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "gym-email-events";

    @Override
    public CompletableFuture<Void> publishUserRegisteredEvent(String email, String username, String type) {
        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("username", username);
        payload.put("type", type);
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(payload);
            return kafkaTemplate.send(TOPIC, jsonMessage)
                    .thenAccept(result -> log.info("Message sent to topic {} successfully", TOPIC))
                    .exceptionally(ex -> {
                        log.error("Failed to send message to kafka topic {}", TOPIC, ex);
                        return null;
                    });
        } catch (JsonProcessingException e) {
            log.error("Could not serialize kafka payload", e);
            return CompletableFuture.completedFuture(null);
        }
    }

}
