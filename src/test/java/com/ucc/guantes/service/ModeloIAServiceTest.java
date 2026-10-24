package com.ucc.guantes.service;

import com.ucc.guantes.dto.ia.EstadisticasIAResponse;
import com.ucc.guantes.dto.ia.ModeloIARequest;
import com.ucc.guantes.dto.ia.ModeloIAResponse;
import com.ucc.guantes.exception.BadRequestException;
import com.ucc.guantes.exception.ResourceNotFoundException;
import com.ucc.guantes.model.ModeloIA;
import com.ucc.guantes.repository.IntentoPracticaRepository;
import com.ucc.guantes.repository.ModeloIARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModeloIAServiceTest {

    @Mock
    private ModeloIARepository modeloIARepository;

    @Mock
    private IntentoPracticaRepository intentoPracticaRepository;

    @InjectMocks
    private ModeloIAService modeloIAService;

    private ModeloIARequest requestValido;
    private ModeloIA modeloExistente;

    @BeforeEach
    void setUp() {
        requestValido = ModeloIARequest.builder()
                .version("v1.0-2026-10")
                .accuracy(0.94)
                .numClases(6)
                .numMuestras(15000)
                .descripcion("Modelo inicial TF.js")
                .activo(true)
                .build();

        modeloExistente = ModeloIA.builder()
                .id(1L)
                .version("v1.0-2026-10")
                .accuracy(0.94)
                .numClases(6)
                .numMuestras(15000)
                .fechaEntrenamiento(LocalDateTime.now())
                .descripcion("Modelo inicial TF.js")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Debe registrar un nuevo modelo exitosamente desactivando el activo anterior si request.activo es true")
    void testRegistrarModeloExitoso() {
        when(modeloIARepository.existsByVersion("v1.0-2026-10")).thenReturn(false);

        ModeloIA modeloPrevioActivo = ModeloIA.builder()
                .id(99L)
                .version("v0.9-beta")
                .activo(true)
                .build();
        when(modeloIARepository.findByActivoTrue()).thenReturn(Optional.of(modeloPrevioActivo));

        when(modeloIARepository.save(any(ModeloIA.class))).thenReturn(modeloExistente);

        ModeloIAResponse respuesta = modeloIAService.registrar(requestValido);

        assertNotNull(respuesta);
        assertEquals("v1.0-2026-10", respuesta.getVersion());
        assertEquals(0.94, respuesta.getAccuracy());
        assertTrue(respuesta.getActivo());

        // Verificar que el modelo previo se desactivó y se guardaron ambos
        assertFalse(modeloPrevioActivo.getActivo());
        verify(modeloIARepository).save(modeloPrevioActivo);
        verify(modeloIARepository, times(2)).save(any(ModeloIA.class));
    }

    @Test
    @DisplayName("Debe lanzar BadRequestException si la versión ya existe")
    void testRegistrarVersionDuplicadaLanzaException() {
        when(modeloIARepository.existsByVersion("v1.0-2026-10")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> modeloIAService.registrar(requestValido));
        verify(modeloIARepository, never()).save(any(ModeloIA.class));
    }

    @Test
    @DisplayName("Debe obtener el modelo activo correctamente")
    void testObtenerActivoExitoso() {
        when(modeloIARepository.findByActivoTrue()).thenReturn(Optional.of(modeloExistente));

        ModeloIAResponse response = modeloIAService.obtenerActivo();

        assertNotNull(response);
        assertEquals("v1.0-2026-10", response.getVersion());
        assertTrue(response.getActivo());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si no hay modelo activo")
    void testObtenerActivoNoEncontrado() {
        when(modeloIARepository.findByActivoTrue()).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> modeloIAService.obtenerActivo());
    }

    @Test
    @DisplayName("Debe listar todas las versiones registradas")
    void testListarModelos() {
        when(modeloIARepository.findAll()).thenReturn(List.of(modeloExistente));

        List<ModeloIAResponse> lista = modeloIAService.listar();

        assertEquals(1, lista.size());
        assertEquals("v1.0-2026-10", lista.get(0).getVersion());
    }

    @Test
    @DisplayName("Debe calcular las estadísticas de IA correctamente")
    void testObtenerEstadisticas() {
        when(intentoPracticaRepository.findPromedioConfianzaGlobal()).thenReturn(0.8546);
        when(intentoPracticaRepository.countIntentosConIA()).thenReturn(80L);
        when(intentoPracticaRepository.count()).thenReturn(100L);

        EstadisticasIAResponse stats = modeloIAService.obtenerEstadisticas();

        assertNotNull(stats);
        assertEquals(0.855, stats.getConfianzaPromedioGlobal());
        assertEquals(80L, stats.getTotalIntentosConIA());
        assertEquals(100L, stats.getTotalIntentosTotales());
        assertEquals(0.8, stats.getTasaUsoIA());
    }
}
