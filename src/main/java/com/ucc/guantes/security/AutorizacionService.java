package com.ucc.guantes.security;

import com.ucc.guantes.model.IntentoPractica;
import com.ucc.guantes.repository.IntentoPracticaRepository;
import com.ucc.guantes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("autorizacion")
@RequiredArgsConstructor
public class AutorizacionService {

    private final UsuarioRepository usuarioRepository;
    private final IntentoPracticaRepository intentoPracticaRepository;

    public boolean esUsuarioActual(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        return usuarioRepository.findByEmail(auth.getName())
                .map(u -> usuarioId.equals(u.getId()))
                .orElse(false);
    }

    public boolean esDuenoDelIntento(Long intentoId) {
        if (intentoId == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        Optional<IntentoPractica> intentoOpt = intentoPracticaRepository.findById(intentoId);
        if (intentoOpt.isEmpty()) {
            return false;
        }
        IntentoPractica intento = intentoOpt.get();
        if (intento.getUsuario() == null) {
            return false;
        }
        return usuarioRepository.findByEmail(auth.getName())
                .map(u -> u.getId() != null && u.getId().equals(intento.getUsuario().getId()))
                .orElse(false);
    }

    public boolean esDocenteOAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DOCENTE".equals(a.getAuthority())
                        || "ROLE_ADMIN".equals(a.getAuthority())
                        || "DOCENTE".equals(a.getAuthority())
                        || "ADMIN".equals(a.getAuthority()));
    }
}
