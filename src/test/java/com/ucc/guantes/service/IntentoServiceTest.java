package com.ucc.guantes.service;

import com.ucc.guantes.dto.intento.IntentoRequest;
import com.ucc.guantes.dto.intento.IntentoResponse;
import com.ucc.guantes.model.*;
import com.ucc.guantes.repository.IntentoPracticaRepository;
import com.ucc.guantes.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntentoServiceTest {

    @Mock
    private IntentoPracticaRepository intentoPracticaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProgresoService progresoService;

    @InjectMocks
    private IntentoService intentoService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Estudiante")
                .apellido("Prueba")
                .email("estudiante@ucc.edu.co")
                .rol(Rol.ESTUDIANTE)
                .build();
    }

    @Test
    @DisplayName("Debe registrar intento con metadatos de IA provistos explícitamente")
    void testRegistrarIntentoConMetadatosIAExplicitos() {
        IntentoRequest request = IntentoRequest.builder()
                .modoPractica(ModoPractica.EVALUACION)
                .puntaje(95.0)
                .duracionSegundos(120)
                .confianzaPromedio(0.92)
                .fuentePrediccion(FuentePrediccion.IA)
                .versionModelo("v1.0-2026-10")
                .detalleIA("{\"manoIzquierda\": 0.91, \"manoDerecha\": 0.93}")
                .build();

        when(usuarioRepository.findByEmail("estudiante@ucc.edu.co")).thenReturn(Optional.of(usuario));
        when(intentoPracticaRepository.save(any(IntentoPractica.class))).thenAnswer(invocation -> {
            IntentoPractica arg = invocation.getArgument(0);
            arg.setId(10L);
            return arg;
        });

        IntentoResponse response = intentoService.registrarIntento(request, "estudiante@ucc.edu.co");

        assertNotNull(response);
        assertEquals(0.92, response.getConfianzaPromedio());
        assertEquals(FuentePrediccion.IA, response.getFuentePrediccion());
        assertEquals("v1.0-2026-10", response.getVersionModelo());
        assertEquals("{\"manoIzquierda\": 0.91, \"manoDerecha\": 0.93}", response.getDetalleIA());

        verify(progresoService).actualizarProgreso(eq(usuario), any(IntentoPractica.class));
    }

    @Test
    @DisplayName("Debe inferir automáticamente CONSENSO si fuente es nula y confianza >= 0.8")
    void testInferenciaAutomaticaConsenso() {
        IntentoRequest request = IntentoRequest.builder()
                .modoPractica(ModoPractica.GUIADO)
                .confianzaPromedio(0.85)
                .versionModelo("v1.0-2026-10")
                .build();

        when(usuarioRepository.findByEmail("estudiante@ucc.edu.co")).thenReturn(Optional.of(usuario));
        ArgumentCaptor<IntentoPractica> captor = ArgumentCaptor.forClass(IntentoPractica.class);
        when(intentoPracticaRepository.save(captor.capture())).thenAnswer(invocation -> {
            IntentoPractica arg = invocation.getArgument(0);
            arg.setId(11L);
            return arg;
        });

        IntentoResponse response = intentoService.registrarIntento(request, "estudiante@ucc.edu.co");

        assertEquals(FuentePrediccion.CONSENSO, captor.getValue().getFuentePrediccion());
        assertEquals(FuentePrediccion.CONSENSO, response.getFuentePrediccion());
    }

    @Test
    @DisplayName("Debe inferir automáticamente MIXTO si fuente es nula y confianza está entre 0.6 y 0.8")
    void testInferenciaAutomaticaMixto() {
        IntentoRequest request = IntentoRequest.builder()
                .modoPractica(ModoPractica.GUIADO)
                .confianzaPromedio(0.72)
                .build();

        when(usuarioRepository.findByEmail("estudiante@ucc.edu.co")).thenReturn(Optional.of(usuario));
        ArgumentCaptor<IntentoPractica> captor = ArgumentCaptor.forClass(IntentoPractica.class);
        when(intentoPracticaRepository.save(captor.capture())).thenAnswer(invocation -> {
            IntentoPractica arg = invocation.getArgument(0);
            arg.setId(12L);
            return arg;
        });

        IntentoResponse response = intentoService.registrarIntento(request, "estudiante@ucc.edu.co");

        assertEquals(FuentePrediccion.MIXTO, captor.getValue().getFuentePrediccion());
        assertEquals(FuentePrediccion.MIXTO, response.getFuentePrediccion());
    }

    @Test
    @DisplayName("Debe inferir automáticamente REGLAS si fuente es nula y confianza es menor a 0.6")
    void testInferenciaAutomaticaReglas() {
        IntentoRequest request = IntentoRequest.builder()
                .modoPractica(ModoPractica.LIBRE)
                .confianzaPromedio(0.45)
                .build();

        when(usuarioRepository.findByEmail("estudiante@ucc.edu.co")).thenReturn(Optional.of(usuario));
        ArgumentCaptor<IntentoPractica> captor = ArgumentCaptor.forClass(IntentoPractica.class);
        when(intentoPracticaRepository.save(captor.capture())).thenAnswer(invocation -> {
            IntentoPractica arg = invocation.getArgument(0);
            arg.setId(13L);
            return arg;
        });

        IntentoResponse response = intentoService.registrarIntento(request, "estudiante@ucc.edu.co");

        assertEquals(FuentePrediccion.REGLAS, captor.getValue().getFuentePrediccion());
        assertEquals(FuentePrediccion.REGLAS, response.getFuentePrediccion());
    }

    @Test
    @DisplayName("Debe permitir guardar sin campos de IA para compatibilidad hacia atrás")
    void testCompatibilidadSinCamposIA() {
        IntentoRequest request = IntentoRequest.builder()
                .modoPractica(ModoPractica.GUIADO)
                .puntaje(80.0)
                .duracionSegundos(60)
                .build();

        when(usuarioRepository.findByEmail("estudiante@ucc.edu.co")).thenReturn(Optional.of(usuario));
        ArgumentCaptor<IntentoPractica> captor = ArgumentCaptor.forClass(IntentoPractica.class);
        when(intentoPracticaRepository.save(captor.capture())).thenAnswer(invocation -> {
            IntentoPractica arg = invocation.getArgument(0);
            arg.setId(14L);
            return arg;
        });

        IntentoResponse response = intentoService.registrarIntento(request, "estudiante@ucc.edu.co");

        assertNotNull(response);
        assertNull(captor.getValue().getConfianzaPromedio());
        assertNull(captor.getValue().getFuentePrediccion());
        assertNull(captor.getValue().getVersionModelo());
        assertNull(captor.getValue().getDetalleIA());
    }
}
