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

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "gym-email-events";

    @Override
    public CompletableFuture<Void> publishUserRegisteredEvent(String email, String username, String type) {
        log.debug("[KAFKA] Preparando evento type={} para email={}, username={}", type, email, username);

        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("username", username);
        payload.put("type", type);

        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.error("[KAFKA] Error serializando payload del evento type={} para email={}", type, email, ex);
            return CompletableFuture.completedFuture(null);
        }

        log.debug("[KAFKA] Publicando en topic={} — payload={}", TOPIC, jsonMessage);

        return kafkaTemplate.send(TOPIC, jsonMessage)
                .thenAccept(result -> log.info(
                        "[KAFKA] Evento publicado exitosamente — topic={}, type={}, email={}, offset={}",
                        TOPIC, type, email,
                        result.getRecordMetadata().offset()
                ))
                .exceptionally(ex -> {
                    log.error("[KAFKA] Error publicando en topic={}, type={}, email={}", TOPIC, type, email, ex);
                    return null;
                });
    }

}