package com.holdings.gym.auth.domain.ports.in;

import com.holdings.gym.auth.domain.model.dto.LoginRequest;
import com.holdings.gym.auth.domain.model.dto.LoginResponse;

import java.util.concurrent.CompletableFuture;

public interface AuthUseCase {
    CompletableFuture<LoginResponse> authenticate(LoginRequest request);
}
