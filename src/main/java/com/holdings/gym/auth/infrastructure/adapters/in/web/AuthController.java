package com.holdings.gym.auth.infrastructure.adapters.in.web;

import com.holdings.gym.auth.domain.model.dto.LoginRequest;
import com.holdings.gym.auth.domain.model.dto.LoginResponse;
import com.holdings.gym.auth.domain.ports.in.AuthUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.holdings.gym.auth.domain.model.dto.RegisterUserRequest;
import com.holdings.gym.auth.domain.ports.in.RegisterUserUseCase;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final RegisterUserUseCase registerUserUseCase;

    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<?>> login(@RequestBody LoginRequest request) {
        return authUseCase.authenticate(request)
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if ("ACCOUNT_NOT_VERIFIED".equals(cause.getMessage())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ACCOUNT_NOT_VERIFIED");
                    }
                    if (cause instanceof IllegalArgumentException) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(cause.getMessage());
                    }
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                });
    }

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<?>> register(@RequestBody @Valid RegisterUserRequest request) {
        return registerUserUseCase.registerUser(request)
                .<ResponseEntity<?>>thenApply(uuid -> ResponseEntity.status(HttpStatus.CREATED).body(uuid))
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof IllegalArgumentException) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getCause().getMessage());
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    @GetMapping("/check-email")
    public CompletableFuture<ResponseEntity<Boolean>> checkEmail(@RequestParam String email) {
        return registerUserUseCase.checkEmailExists(email)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/check-nick")
    public CompletableFuture<ResponseEntity<Boolean>> checkNick(@RequestParam String nick) {
        return registerUserUseCase.checkNickExists(nick)
                .thenApply(ResponseEntity::ok);
    }
}
