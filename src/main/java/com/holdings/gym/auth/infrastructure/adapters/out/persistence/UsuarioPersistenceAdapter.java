package com.holdings.gym.auth.infrastructure.adapters.out.persistence;

import com.holdings.gym.auth.domain.ports.out.UserPersistencePort;
import com.holdings.gym.auth.infrastructure.adapters.out.persistence.entity.UsuarioEntity;
import com.holdings.gym.auth.infrastructure.adapters.out.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UserPersistencePort {

    private final UsuarioRepository usuarioRepository;

    @Override
    public CompletableFuture<Boolean> existsByEmail(String email) {
        return usuarioRepository.existsByEmailUsuario(email).toFuture();
    }

    @Override
    public CompletableFuture<Boolean> existsByNick(String nick) {
        return usuarioRepository.existsByNickUsuario(nick).toFuture();
    }

    @Override
    public CompletableFuture<UUID> saveUser(String nombres, String apellidos, String nickUsuario, String email, String passwordHasheada) {
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
                .build();

        return usuarioRepository.save(entity)
                .map(UsuarioEntity::getUuidUsuario)
                .toFuture();
    }

    @Override
    public CompletableFuture<com.holdings.gym.auth.domain.model.dto.UserAuthData> findByEmail(String email) {
        return usuarioRepository.findByEmailUsuario(email)
                .map(entity -> com.holdings.gym.auth.domain.model.dto.UserAuthData.builder()
                        .uuidUsuario(entity.getUuidUsuario())
                        .uuidEmpresa(entity.getUuidEmpresa())
                        .email(entity.getEmailUsuario())
                        .passwordHasheada(entity.getPasswordUsuario())
                        .cuentaVerificada(entity.getCuentaVerificada())
                        .roles("ADMIN") // Default for now
                        .build())
                .toFuture();
    }
}
