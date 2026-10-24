package com.ucc.guantes.config;

import com.ucc.guantes.model.Rol;
import com.ucc.guantes.model.Usuario;
import com.ucc.guantes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos semilla al arrancar la aplicación.
 * Garantiza la existencia del usuario administrador inicial si no existe previamente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        final String adminEmail = "admin@ucc.edu.co";

        if (!usuarioRepository.existsByEmail(adminEmail)) {
            Usuario admin = Usuario.builder()
                    .nombre("Administrador")
                    .apellido("Sistema")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123456"))
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .build();

            usuarioRepository.save(admin);
            log.info("✅ Usuario ADMIN inicial creado exitosamente: {}", adminEmail);
        } else {
            log.info("ℹ️ Usuario ADMIN ({}) ya existe en la base de datos.", adminEmail);
        }
    }
}
