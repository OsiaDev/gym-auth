package com.holdings.gym.auth.domain.ports.out;

import java.util.concurrent.CompletableFuture;

public interface EventPublisherPort {
    CompletableFuture<Void> publishUserRegisteredEvent(String email, String username, String type);
}
