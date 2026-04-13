package com.holdings.gym.auth.infrastructure.adapters.in.web;

import com.holdings.gym.auth.domain.model.dto.LoginRequest;
import com.holdings.gym.auth.domain.ports.in.AuthUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final RegisterUserUseCase registerUserUseCase;

    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<?>> login(@RequestBody LoginRequest request) {
        log.info("[AUTH][LOGIN] Solicitud de login recibida para email={}", request.getEmail());

        return authUseCase.authenticate(request)
                .<ResponseEntity<?>>thenApply(response -> {
                    log.info("[AUTH][LOGIN] Login exitoso para email={}", request.getEmail());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

                    if ("ACCOUNT_NOT_VERIFIED".equals(cause.getMessage())) {
                        log.warn("[AUTH][LOGIN] Cuenta no verificada para email={}", request.getEmail());
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ACCOUNT_NOT_VERIFIED");
                    }
                    if (cause instanceof IllegalArgumentException) {
                        log.warn("[AUTH][LOGIN] Credenciales inválidas para email={} — {}", request.getEmail(), cause.getMessage());
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(cause.getMessage());
                    }

                    log.error("[AUTH][LOGIN] Error inesperado en login para email={}", request.getEmail(), cause);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                });
    }

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<?>> register(@RequestBody @Valid RegisterUserRequest request) {
        log.info("[AUTH][REGISTER] Solicitud de registro recibida para email={}, nick={}", request.getEmail(), request.getNickUsuario());

        return registerUserUseCase.registerUser(request)
                .<ResponseEntity<?>>thenApply(uuid -> {
                    log.info("[AUTH][REGISTER] Usuario registrado exitosamente — email={}, uuid={}", request.getEmail(), uuid);
                    return ResponseEntity.status(HttpStatus.CREATED).body(uuid);
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

                    if (cause instanceof IllegalArgumentException) {
                        log.warn("[AUTH][REGISTER] Validación fallida para email={} — {}", request.getEmail(), cause.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cause.getMessage());
                    }

                    log.error("[AUTH][REGISTER] Error inesperado registrando email={}", request.getEmail(), cause);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    @GetMapping("/check-email")
    public CompletableFuture<ResponseEntity<Boolean>> checkEmail(@RequestParam String email) {
        log.debug("[AUTH][CHECK-EMAIL] Verificando disponibilidad de email={}", email);

        return registerUserUseCase.checkEmailExists(email)
                .thenApply(exists -> {
                    log.debug("[AUTH][CHECK-EMAIL] email={} exists={}", email, exists);
                    return ResponseEntity.ok(exists);
                });
    }

    @GetMapping("/check-nick")
    public CompletableFuture<ResponseEntity<Boolean>> checkNick(@RequestParam String nick) {
        log.debug("[AUTH][CHECK-NICK] Verificando disponibilidad de nick={}", nick);

        return registerUserUseCase.checkNickExists(nick)
                .thenApply(exists -> {
                    log.debug("[AUTH][CHECK-NICK] nick={} exists={}", nick, exists);
                    return ResponseEntity.ok(exists);
                });
    }

}