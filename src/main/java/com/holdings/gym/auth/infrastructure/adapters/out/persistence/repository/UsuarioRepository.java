package com.holdings.gym.auth.infrastructure.adapters.out.persistence.repository;

import com.holdings.gym.auth.infrastructure.adapters.out.persistence.entity.UsuarioEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UsuarioRepository extends R2dbcRepository<UsuarioEntity, UUID> {
    
    Mono<Boolean> existsByEmailUsuario(String emailUsuario);
    
    Mono<Boolean> existsByNickUsuario(String nickUsuario);

    Mono<UsuarioEntity> findByEmailUsuario(String emailUsuario);

}
