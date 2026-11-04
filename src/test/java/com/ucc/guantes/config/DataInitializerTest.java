package com.ucc.guantes.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ucc.guantes.model.Rol;
import com.ucc.guantes.model.Usuario;
import com.ucc.guantes.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("No crea administrador y registra advertencia sin exponer contraseña cuando falta ADMIN_EMAIL o ADMIN_PASSWORD")
    void noCreaAdminSiFaltanVariables() {
        Logger logger = (Logger) LoggerFactory.getLogger(DataInitializer.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            ReflectionTestUtils.setField(dataInitializer, "adminEmail", "");
            ReflectionTestUtils.setField(dataInitializer, "adminPassword", "ClaveSecretaNoExponer123!");

            dataInitializer.run();

            verify(usuarioRepository, never()).save(any());
            assertTrue(listAppender.list.stream().anyMatch(ev -> ev.getLevel() == Level.WARN));
            assertTrue(listAppender.list.stream().noneMatch(ev -> ev.getFormattedMessage().contains("ClaveSecretaNoExponer123!")));
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("Crea administrador desde ADMIN_EMAIL y ADMIN_PASSWORD cuando no existe en base de datos")
    void creaAdminDesdeVariablesDeEntorno() {
        ReflectionTestUtils.setField(dataInitializer, "adminEmail", "admin.institucional@ucc.edu.co");
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "ClaveSeguraPrueba#2026");

        when(usuarioRepository.existsByEmail("admin.institucional@ucc.edu.co")).thenReturn(false);
        when(passwordEncoder.encode("ClaveSeguraPrueba#2026")).thenReturn("$2a$10$hashDePrueba");

        dataInitializer.run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        Usuario guardado = captor.getValue();
        assertEquals("admin.institucional@ucc.edu.co", guardado.getEmail());
        assertEquals("$2a$10$hashDePrueba", guardado.getPassword());
        assertEquals(Rol.ADMIN, guardado.getRol());
        assertTrue(guardado.getActivo());
        assertFalse(guardado.getPassword().contains("ClaveSeguraPrueba#2026"));
    }
}
