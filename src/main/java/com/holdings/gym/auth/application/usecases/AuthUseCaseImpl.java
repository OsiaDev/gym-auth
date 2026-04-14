package com.holdings.gym.auth.application.usecases;

import com.holdings.gym.auth.domain.model.dto.LoginRequest;
import com.holdings.gym.auth.domain.model.dto.LoginResponse;
import com.holdings.gym.auth.domain.ports.in.AuthUseCase;
import com.holdings.gym.auth.domain.ports.out.UserPersistencePort;
import com.holdings.gym.auth.infrastructure.security.RsaKeyProvider;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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
        log.debug("[AUTH] Iniciando autenticación — email={}", request.getEmail());

        return userPersistencePort.findByEmail(request.getEmail())
                .thenApply(user -> {
                    if (user == null) {
                        log.warn("[AUTH] Usuario no encontrado — email={}", request.getEmail());
                        throw new IllegalArgumentException("Credenciales inválidas");
                    }

                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHasheada())) {
                        log.warn("[AUTH] Contraseña incorrecta — email={}", request.getEmail());
                        throw new IllegalArgumentException("Credenciales inválidas");
                    }

                    if (!Boolean.TRUE.equals(user.getCuentaVerificada())) {
                        log.warn("[AUTH] Cuenta no verificada — email={}, uuid={}", request.getEmail(), user.getUuidUsuario());
                        throw new IllegalStateException("ACCOUNT_NOT_VERIFIED");
                    }

                    log.debug("[AUTH] Credenciales válidas — email={}, uuid={}", request.getEmail(), user.getUuidUsuario());

                    Instant now = Instant.now();
                    Instant expiration = now.plus(8, ChronoUnit.HOURS);

                    String token;
                    try {
                        token = Jwts.builder()
                                .header().add("kid", rsaKeyProvider.getKeyId()).and()
                                .subject(user.getUuidUsuario().toString())
                                .issuer(issuerUri)
                                .claim("empresa_id", user.getEmpresaId() != null ? user.getEmpresaId() : "") // Ahora viene de la DB
                                .claim("roles", user.getRoles())
                                .claim("nick", user.getNickUsuario() != null ? user.getNickUsuario() : "")
                                .issuedAt(Date.from(now))
                                .expiration(Date.from(expiration))
                                .signWith(rsaKeyProvider.getPrivateKey(), Jwts.SIG.RS256)
                                .compact();
                    } catch (Exception ex) {
                        log.error("[AUTH] Error al firmar el JWT — email={}, uuid={}", request.getEmail(), user.getUuidUsuario(), ex);
                        throw new RuntimeException("Error al generar el token de acceso", ex);
                    }

                    log.info("[AUTH] JWT generado exitosamente — email={}, uuid={}, expiresAt={}", request.getEmail(), user.getUuidUsuario(), expiration);

                    return LoginResponse.builder()
                            .accessToken(token)
                            .tokenType("Bearer")
                            .expiresIn(28800L)
                            .build();
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (!(cause instanceof IllegalArgumentException) && !(cause instanceof IllegalStateException)) {
                        log.error("[AUTH] Error inesperado durante autenticación — email={}", request.getEmail(), cause);
                    }
                    throw (cause instanceof RuntimeException re) ? re : new RuntimeException(cause);
                });
    }

}