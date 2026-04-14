package com.holdings.gym.auth.infrastructure.adapters.out.persistence;

import com.holdings.gym.auth.domain.model.dto.UserAuthData;
import com.holdings.gym.auth.domain.ports.out.UserPersistencePort;
import com.holdings.gym.auth.infrastructure.adapters.out.persistence.entity.UsuarioEntity;
import com.holdings.gym.auth.infrastructure.adapters.out.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UserPersistencePort {

    private final UsuarioRepository usuarioRepository;
    private final org.springframework.r2dbc.core.DatabaseClient databaseClient;

    @Override
    public CompletableFuture<Boolean> existsByEmail(String email) {
        log.debug("[PERSISTENCE] Consultando existencia por email={}", email);
        return usuarioRepository.existsByEmailUsuario(email)
                .doOnNext(exists -> log.debug("[PERSISTENCE] existsByEmail={} result={}", email, exists))
                .doOnError(ex -> log.error("[PERSISTENCE] Error consultando existsByEmail={}", email, ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Boolean> existsByNick(String nick) {
        log.debug("[PERSISTENCE] Consultando existencia por nick={}", nick);
        return usuarioRepository.existsByNickUsuario(nick)
                .doOnNext(exists -> log.debug("[PERSISTENCE] existsByNick={} result={}", nick, exists))
                .doOnError(ex -> log.error("[PERSISTENCE] Error consultando existsByNick={}", nick, ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<UUID> saveUser(String nombres, String apellidos, String nickUsuario, String email, String passwordHasheada) {
        log.debug("[PERSISTENCE] Guardando nuevo usuario — email={}, nick={}", email, nickUsuario);

        UsuarioEntity entity = UsuarioEntity.builder()
                .uuidUsuario(UUID.randomUUID())
                .nombresUsuario(nombres)
                .apellidosUsuario(apellidos)
                .nickUsuario(nickUsuario)
                .emailUsuario(email)
                .passwordUsuario(passwordHasheada)
                .estadoUsuario(true)
                .cuentaVerificada(false)
                .createdAt(LocalDateTime.now())
                .isNew(true)
                .build();

        return usuarioRepository.save(entity)
                .doOnNext(saved -> log.info("[PERSISTENCE] Usuario guardado exitosamente — uuid={}, email={}", saved.getUuidUsuario(), email))
                .doOnError(ex -> log.error("[PERSISTENCE] Error al guardar usuario — email={}, nick={}", email, nickUsuario, ex))
                .map(UsuarioEntity::getUuidUsuario)
                .toFuture();
    }

    @Override
    public CompletableFuture<UserAuthData> findByEmail(String email) {
        log.debug("[PERSISTENCE] Buscando usuario por email={}", email);

        return usuarioRepository.findByEmailUsuario(email)
                .doOnNext(entity -> log.debug("[PERSISTENCE] Usuario encontrado — email={}, uuid={}", email, entity.getUuidUsuario()))
                .doOnError(ex -> log.error("[PERSISTENCE] Error buscando usuario por email={}", email, ex))
                .flatMap(entity -> databaseClient.sql("SELECT uuidempresa, STRING_AGG(rolUsuario::text, ',') AS roles_agg FROM rol_usuario WHERE uuidusuario = :uuid GROUP BY uuidempresa LIMIT 1")
                        .bind("uuid", entity.getUuidUsuario())
                        .map(row -> {
                            UUID empresaId = row.get("uuidempresa", UUID.class);
                            String roles = row.get("roles_agg", String.class);
                            return UserAuthData.builder()
                                    .uuidUsuario(entity.getUuidUsuario())
                                    .nickUsuario(entity.getNickUsuario())
                                    .email(entity.getEmailUsuario())
                                    .passwordHasheada(entity.getPasswordUsuario())
                                    .cuentaVerificada(entity.getCuentaVerificada())
                                    .roles(roles != null ? roles : "")
                                    .empresaId(empresaId != null ? empresaId.toString() : "")
                                    .build();
                        })
                        .first()
                        .defaultIfEmpty(UserAuthData.builder()
                                .uuidUsuario(entity.getUuidUsuario())
                                .nickUsuario(entity.getNickUsuario())
                                .email(entity.getEmailUsuario())
                                .passwordHasheada(entity.getPasswordUsuario())
                                .cuentaVerificada(entity.getCuentaVerificada())
                                .roles("")
                                .empresaId("")
                                .build())
                )
                .toFuture();
    }

}