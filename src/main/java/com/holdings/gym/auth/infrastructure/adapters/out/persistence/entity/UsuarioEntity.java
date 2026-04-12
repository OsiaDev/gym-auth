package com.holdings.gym.auth.infrastructure.adapters.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("usuario")
public class UsuarioEntity {

    @Id
    @Column("uuidusuario") // In R2DBC mapping maps case-insensitive but sometimes exact column depends on db naming
    private UUID uuidUsuario;

    @Column("uuidempresa")
    private UUID uuidEmpresa;

    @Column("uuidsede")
    private UUID uuidSede;

    @Column("nombresusuario")
    private String nombresUsuario;

    @Column("apellidosusuario")
    private String apellidosUsuario;

    @Column("nickusuario")
    private String nickUsuario;

    @Column("emailusuario")
    private String emailUsuario;

    @Column("passwordusuario")
    private String passwordUsuario;

    @Column("estadousuario")
    private Boolean estadoUsuario;

    @Column("cuenta_verificada")
    private Boolean cuentaVerificada;

    @Column("created_at")
    private LocalDateTime createdAt;

}
