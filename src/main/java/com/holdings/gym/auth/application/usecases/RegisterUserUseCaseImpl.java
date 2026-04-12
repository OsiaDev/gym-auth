package com.holdings.gym.auth.application.usecases;

import com.holdings.gym.auth.domain.model.dto.RegisterUserRequest;
import com.holdings.gym.auth.domain.ports.in.RegisterUserUseCase;
import com.holdings.gym.auth.domain.ports.out.EventPublisherPort;
import com.holdings.gym.auth.domain.ports.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

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
        // 1. Password Match Validation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Las contraseñas no coinciden"));
        }

        // 2. Password Complexity Validation
        if (!pattern.matcher(request.getPassword()).matches()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "La contraseña debe tener entre 8 y 20 caracteres, contener al menos una mayúscula, una minúscula, un número y un carácter especial"));
        }

        // 3. Check for existence (email and nick)
        return userPersistencePort.existsByEmail(request.getEmail())
                .thenCompose(existsEmail -> {
                    if (existsEmail) {
                        return CompletableFuture.failedFuture(new IllegalArgumentException("El correo electrónico ya está registrado"));
                    }
                    return userPersistencePort.existsByNick(request.getNickUsuario());
                })
                .thenCompose(existsNick -> {
                    if (existsNick) {
                        return CompletableFuture.failedFuture(new IllegalArgumentException("El nombre de usuario (nick) ya está en uso"));
                    }

                    // 4. Encode Password & Save
                    String hashedPassword = passwordEncoder.encode(request.getPassword());

                    return userPersistencePort.saveUser(
                            request.getNombres(),
                            request.getApellidos(),
                            request.getNickUsuario(),
                            request.getEmail(),
                            hashedPassword
                    );
                })
                .thenCompose(uuid -> {
                    // 5. Send Kafka Event
                    return eventPublisherPort.publishUserRegisteredEvent(request.getEmail(), request.getNickUsuario(), "USER_REGISTERED")
                            .thenApply(v -> uuid.toString());
                });
    }

    @Override
    public CompletableFuture<Boolean> checkEmailExists(String email) {
        return userPersistencePort.existsByEmail(email);
    }

    @Override
    public CompletableFuture<Boolean> checkNickExists(String nick) {
        return userPersistencePort.existsByNick(nick);
    }
}
