package com.ucc.guantes.service;

import com.ucc.guantes.dto.progreso.ProgresoResponse;
import com.ucc.guantes.exception.ResourceNotFoundException;
import com.ucc.guantes.model.IntentoPractica;
import com.ucc.guantes.model.Progreso;
import com.ucc.guantes.model.Usuario;
import com.ucc.guantes.repository.IntentoPracticaRepository;
import com.ucc.guantes.repository.ProgresoRepository;
import com.ucc.guantes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgresoService {

    private final ProgresoRepository progresoRepository;
    private final IntentoPracticaRepository intentoPracticaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public ProgresoResponse obtenerProgresoPorUsuario(Long usuarioId) {
        Progreso progreso = progresoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    Usuario usuario = usuarioRepository.findById(usuarioId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));
                    return crearProgresoInicial(usuario);
                });

        return mapToResponse(progreso);
    }

    @Transactional(readOnly = true)
    public ProgresoResponse obtenerProgresoPorEmail(String email) {
        Progreso progreso = progresoRepository.findByUsuarioEmail(email)
                .orElseGet(() -> {
                    Usuario usuario = usuarioRepository.findByEmail(email)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", email));
                    return crearProgresoInicial(usuario);
                });

        return mapToResponse(progreso);
    }

    @Transactional
    public void actualizarProgreso(Usuario usuario, IntentoPractica intento) {
        Progreso progreso = progresoRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> crearProgresoInicial(usuario));

        List<IntentoPractica> intentos = intentoPracticaRepository.findByUsuarioIdOrderByFechaInicioDesc(usuario.getId());

        int totalIntentos = intentos.size();
        int intentosExitosos = (int) intentos.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCompletado()))
                .count();

        int tiempoTotal = intentos.stream()
                .mapToInt(i -> i.getDuracionSegundos() != null ? i.getDuracionSegundos() : 0)
                .sum();

        double promedio = intentos.stream()
                .filter(i -> i.getPuntaje() != null)
                .mapToDouble(IntentoPractica::getPuntaje)
                .average()
                .orElse(0.0);

        double mejorPuntaje = intentos.stream()
                .filter(i -> i.getPuntaje() != null)
                .mapToDouble(IntentoPractica::getPuntaje)
                .max()
                .orElse(0.0);

        progreso.setIntentosTotales(totalIntentos);
        progreso.setIntentosExitosos(intentosExitosos);
        progreso.setTiempoTotalSegundos(tiempoTotal);
        progreso.setPromedioPuntaje(Math.round(promedio * 100.0) / 100.0);
        progreso.setMejorPuntaje(Math.round(mejorPuntaje * 100.0) / 100.0);
        progreso.setNivelAlcanzado(calcularNivel(promedio));
        progreso.setUltimoAcceso(LocalDateTime.now());

        progresoRepository.save(progreso);
    }

    @Transactional
    public ProgresoResponse recalcularProgreso(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));

        Progreso progreso = progresoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearProgresoInicial(usuario));

        List<IntentoPractica> intentos = intentoPracticaRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId);

        int totalIntentos = intentos.size();
        int intentosExitosos = (int) intentos.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCompletado()))
                .count();

        int tiempoTotal = intentos.stream()
                .mapToInt(i -> i.getDuracionSegundos() != null ? i.getDuracionSegundos() : 0)
                .sum();

        double promedio = intentos.stream()
                .filter(i -> i.getPuntaje() != null)
                .mapToDouble(IntentoPractica::getPuntaje)
                .average()
                .orElse(0.0);

        double mejorPuntaje = intentos.stream()
                .filter(i -> i.getPuntaje() != null)
                .mapToDouble(IntentoPractica::getPuntaje)
                .max()
                .orElse(0.0);

        progreso.setIntentosTotales(totalIntentos);
        progreso.setIntentosExitosos(intentosExitosos);
        progreso.setTiempoTotalSegundos(tiempoTotal);
        progreso.setPromedioPuntaje(Math.round(promedio * 100.0) / 100.0);
        progreso.setMejorPuntaje(Math.round(mejorPuntaje * 100.0) / 100.0);
        progreso.setNivelAlcanzado(calcularNivel(promedio));
        progreso.setUltimoAcceso(LocalDateTime.now());

        progresoRepository.save(progreso);
        return mapToResponse(progreso);
    }

    private Progreso crearProgresoInicial(Usuario usuario) {
        Progreso progreso = Progreso.builder()
                .usuario(usuario)
                .intentosTotales(0)
                .intentosExitosos(0)
                .tiempoTotalSegundos(0)
                .promedioPuntaje(0.0)
                .mejorPuntaje(0.0)
                .nivelAlcanzado("PRINCIPIANTE")
                .ultimoAcceso(LocalDateTime.now())
                .build();
        return progresoRepository.save(progreso);
    }

    private String calcularNivel(double promedio) {
        if (promedio >= 85.0) {
            return "AVANZADO";
        } else if (promedio >= 60.0) {
            return "INTERMEDIO";
        } else {
            return "PRINCIPIANTE";
        }
    }

    public ProgresoResponse mapToResponse(Progreso progreso) {
        return ProgresoResponse.builder()
                .id(progreso.getId())
                .usuarioId(progreso.getUsuario().getId())
                .usuarioNombre(progreso.getUsuario().getNombre() + " " + progreso.getUsuario().getApellido())
                .usuarioEmail(progreso.getUsuario().getEmail())
                .intentosTotales(progreso.getIntentosTotales())
                .intentosExitosos(progreso.getIntentosExitosos())
                .tiempoTotalSegundos(progreso.getTiempoTotalSegundos())
                .promedioPuntaje(progreso.getPromedioPuntaje())
                .mejorPuntaje(progreso.getMejorPuntaje())
                .nivelAlcanzado(progreso.getNivelAlcanzado())
                .ultimoAcceso(progreso.getUltimoAcceso())
                .build();
    }
}
