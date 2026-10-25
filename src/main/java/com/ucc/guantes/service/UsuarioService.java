package com.ucc.guantes.service;

import com.ucc.guantes.dto.usuario.UsuarioRequest;
import com.ucc.guantes.dto.usuario.UsuarioResponse;
import com.ucc.guantes.dto.usuario.UsuarioUpdateRequest;
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
    private final ProgresoService progresoService;

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
    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya se encuentra registrado: " + request.getEmail());
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("La contraseña es obligatoria para nuevos usuarios");
        }

        String apellido = request.getApellido() != null ? request.getApellido() : "";

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(apellido)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .semestre(request.getSemestre())
                .grupo(request.getGrupo())
                .rol(request.getRol() != null ? request.getRol() : Rol.ESTUDIANTE)
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        progresoService.crearProgresoInicial(guardado);

        return mapToResponse(guardado);
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        return crearUsuario(request);
    }

    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));

        if (!usuario.getEmail().equalsIgnoreCase(request.getEmail()) && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya se encuentra en uso por otro usuario: " + request.getEmail());
        }

        usuario.setNombre(request.getNombre());
        if (request.getApellido() != null) {
            usuario.setApellido(request.getApellido());
        }
        usuario.setEmail(request.getEmail());
        usuario.setSemestre(request.getSemestre());
        usuario.setGrupo(request.getGrupo());

        if (request.getRol() != null) {
            usuario.setRol(request.getRol());
        }

        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        return mapToResponse(actualizado);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        UsuarioUpdateRequest updateRequest = UsuarioUpdateRequest.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .semestre(request.getSemestre())
                .grupo(request.getGrupo())
                .rol(request.getRol())
                .password(request.getPassword())
                .activo(request.getActivo())
                .build();
        return actualizarUsuario(id, updateRequest);
    }

    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
        usuarioRepository.delete(usuario);
    }

    @Transactional
    public void eliminar(Long id) {
        eliminarUsuario(id);
    }

    public UsuarioResponse mapToResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .semestre(usuario.getSemestre())
                .grupo(usuario.getGrupo())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .fechaRegistro(usuario.getFechaCreacion())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }
}
