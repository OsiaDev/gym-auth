package com.holdings.gym.auth.domain.ports.out;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserPersistencePort {

    CompletableFuture<Boolean> existsByEmail(String email);

    CompletableFuture<Boolean> existsByNick(String nick);

    CompletableFuture<UUID> saveUser(String nombres, String apellidos, String nickUsuario, String email, String passwordHasheada);

    CompletableFuture<com.holdings.gym.auth.domain.model.dto.UserAuthData> findByEmail(String email);
}
