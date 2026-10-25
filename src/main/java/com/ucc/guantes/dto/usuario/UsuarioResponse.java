package com.ucc.guantes.dto.usuario;

import com.ucc.guantes.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private Integer semestre;
    private String grupo;
    private Rol rol;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
