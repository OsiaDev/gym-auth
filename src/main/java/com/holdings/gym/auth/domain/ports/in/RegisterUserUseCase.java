package com.holdings.gym.auth.domain.ports.in;

import com.holdings.gym.auth.domain.model.dto.RegisterUserRequest;

import java.util.concurrent.CompletableFuture;

public interface RegisterUserUseCase {

    CompletableFuture<String> registerUser(RegisterUserRequest request);

    CompletableFuture<Boolean> checkEmailExists(String email);
    
    CompletableFuture<Boolean> checkNickExists(String nick);

}
