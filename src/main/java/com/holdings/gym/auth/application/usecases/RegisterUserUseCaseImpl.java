package com.holdings.gym.auth.application.usecases;

import com.holdings.gym.auth.domain.model.dto.RegisterUserRequest;
import com.holdings.gym.auth.domain.ports.in.RegisterUserUseCase;
import com.holdings.gym.auth.domain.ports.out.EventPublisherPort;
import com.holdings.gym.auth.domain.ports.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserPersistencePort userPersistencePort;
    private final EventPublisherPort eventPublisherPort;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+{}\\[\\]:;<>,.?~\\\\/-]).{8,20}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    @Override
    public CompletableFuture<String> registerUser(RegisterUserRequest request) {
        log.debug("[REGISTER] Iniciando registro — email={}, nick={}", request.getEmail(), request.getNickUsuario());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("[REGISTER] Las contraseñas no coinciden — email={}", request.getEmail());
            return CompletableFuture.failedFuture(new IllegalArgumentException("Las contraseñas no coinciden"));
        }

        if (!pattern.matcher(request.getPassword()).matches()) {
            log.warn("[REGISTER] Contraseña no cumple complejidad requerida — email={}", request.getEmail());
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "La contraseña debe tener entre 8 y 20 caracteres, contener al menos una mayúscula, una minúscula, un número y un carácter especial"));
        }

        return userPersistencePort.existsByEmail(request.getEmail())
                .thenCompose(existsEmail -> {
                    if (existsEmail) {
                        log.warn("[REGISTER] Email ya registrado — email={}", request.getEmail());
                        return CompletableFuture.failedFuture(new IllegalArgumentException("El correo electrónico ya está registrado"));
                    }
                    log.debug("[REGISTER] Email disponible — email={}", request.getEmail());
                    return userPersistencePort.existsByNick(request.getNickUsuario());
                })
                .thenCompose(existsNick -> {
                    if (existsNick) {
                        log.warn("[REGISTER] Nick ya en uso — nick={}", request.getNickUsuario());
                        return CompletableFuture.failedFuture(new IllegalArgumentException("El nombre de usuario (nick) ya está en uso"));
                    }
                    log.debug("[REGISTER] Nick disponible — nick={}", request.getNickUsuario());

                    String hashedPassword = passwordEncoder.encode(request.getPassword());
                    log.debug("[REGISTER] Contraseña hasheada correctamente — email={}", request.getEmail());

                    return userPersistencePort.saveUser(
                            request.getNombres(),
                            request.getApellidos(),
                            request.getNickUsuario(),
                            request.getEmail(),
                            hashedPassword
                    );
                })
                .thenCompose(uuid -> {
                    log.info("[REGISTER] Usuario persistido — uuid={}, email={}", uuid, request.getEmail());
                    return eventPublisherPort
                            .publishUserRegisteredEvent(request.getEmail(), request.getNickUsuario(), "USER_REGISTERED")
                            .thenApply(v -> {
                                log.info("[REGISTER] Evento USER_REGISTERED publicado — uuid={}", uuid);
                                return uuid.toString();
                            });
                })
                .exceptionally(ex -> {
                    // Solo loguear errores inesperados; los de negocio ya se loguean arriba
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (!(cause instanceof IllegalArgumentException)) {
                        log.error("[REGISTER] Error inesperado durante el registro — email={}", request.getEmail(), cause);
                    }
                    throw (cause instanceof RuntimeException re) ? re : new RuntimeException(cause);
                });
    }

    @Override
    public CompletableFuture<Boolean> checkEmailExists(String email) {
        log.debug("[REGISTER] Consultando existencia de email={}", email);
        return userPersistencePort.existsByEmail(email)
                .thenApply(exists -> {
                    log.debug("[REGISTER] Resultado check email={} exists={}", email, exists);
                    return exists;
                });
    }

    @Override
    public CompletableFuture<Boolean> checkNickExists(String nick) {
        log.debug("[REGISTER] Consultando existencia de nick={}", nick);
        return userPersistencePort.existsByNick(nick)
                .thenApply(exists -> {
                    log.debug("[REGISTER] Resultado check nick={} exists={}", nick, exists);
                    return exists;
                });
    }

}