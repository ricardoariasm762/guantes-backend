package com.ucc.guantes.service;

import com.ucc.guantes.dto.usuario.UsuarioRequest;
import com.ucc.guantes.dto.usuario.UsuarioResponse;
import com.ucc.guantes.exception.BadRequestException;
import com.ucc.guantes.exception.ResourceNotFoundException;
import com.ucc.guantes.model.Rol;
import com.ucc.guantes.model.Usuario;
import com.ucc.guantes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
        return mapToResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", email));
        return mapToResponse(usuario);
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya se encuentra registrado: " + request.getEmail());
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("La contraseña es obligatoria para nuevos usuarios");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol() != null ? request.getRol() : Rol.ESTUDIANTE)
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return mapToResponse(guardado);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));

        if (!usuario.getEmail().equalsIgnoreCase(request.getEmail()) && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya se encuentra en uso por otro usuario: " + request.getEmail());
        }

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRol() != null) {
            usuario.setRol(request.getRol());
        }

        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        return mapToResponse(actualizado);
    }

    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuario", "id", id);
        }
        usuarioRepository.deleteById(id);
    }

    public UsuarioResponse mapToResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }
}
