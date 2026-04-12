package com.holdings.gym.auth.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthData {
    private UUID uuidUsuario;
    private UUID uuidEmpresa;
    private String email;
    private String passwordHasheada;
    private Boolean cuentaVerificada;
    private String roles;
}
