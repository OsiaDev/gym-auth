package com.holdings.gym.auth.application.usecases;

import com.holdings.gym.auth.domain.model.dto.LoginRequest;
import com.holdings.gym.auth.domain.model.dto.LoginResponse;
import com.holdings.gym.auth.domain.ports.in.AuthUseCase;
import com.holdings.gym.auth.domain.ports.in.AuthUseCase;
import com.holdings.gym.auth.domain.ports.out.UserPersistencePort;
import com.holdings.gym.auth.domain.model.dto.UserAuthData;
import com.holdings.gym.auth.infrastructure.security.RsaKeyProvider;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUseCaseImpl implements AuthUseCase {

    private final RsaKeyProvider rsaKeyProvider;
    private final UserPersistencePort userPersistencePort;
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.issuer-uri:http://localhost:8081}")
    private String issuerUri;

    @Override
    public CompletableFuture<LoginResponse> authenticate(LoginRequest request) {
        log.info("Procesando intento de inicio de sesión para el email: {}", request.getEmail());
        return userPersistencePort.findByEmail(request.getEmail())
                .thenApply(user -> {
                    if (user == null) {
                        log.warn("Login fallido: Usuario no encontrado para el email: {}", request.getEmail());
                        throw new IllegalArgumentException("Credenciales inválidas");
                    }
                    
                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHasheada())) {
                        log.warn("Login fallido: Contraseña incorrecta para el email: {}", request.getEmail());
                        throw new IllegalArgumentException("Credenciales inválidas");
                    }
                    
                    if (!Boolean.TRUE.equals(user.getCuentaVerificada())) {
                        log.warn("Login fallido: Cuenta no verificada para el email: {}", request.getEmail());
                        throw new IllegalStateException("ACCOUNT_NOT_VERIFIED");
                    }

                    log.info("Login exitoso para el email: {}", request.getEmail());

                    Instant now = Instant.now();
                    Instant expiration = now.plus(8, ChronoUnit.HOURS);
                    
                    String token = Jwts.builder()
                            .header().add("kid", rsaKeyProvider.getKeyId()).and()
                            .subject(user.getUuidUsuario().toString())
                            .issuer(issuerUri)
                            .claim("empresa_id", user.getUuidEmpresa() != null ? user.getUuidEmpresa().toString() : "")
                            .claim("roles", user.getRoles())
                            .issuedAt(Date.from(now))
                            .expiration(Date.from(expiration))
                            .signWith(rsaKeyProvider.getPrivateKey(), Jwts.SIG.RS256)
                            .compact();

                    return LoginResponse.builder()
                            .accessToken(token)
                            .tokenType("Bearer")
                            .expiresIn(28800L)
                            .build();
                });
    }
}
