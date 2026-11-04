package com.ucc.guantes.config;

import com.ucc.guantes.model.Rol;
import com.ucc.guantes.model.Usuario;
import com.ucc.guantes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos semilla al arrancar la aplicación.
 * Crea el usuario administrador inicial únicamente si se proveen ADMIN_EMAIL y ADMIN_PASSWORD.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        String emailNormalizado = adminEmail == null ? "" : adminEmail.trim();

        if (emailNormalizado.isEmpty() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("Variables de entorno ADMIN_EMAIL o ADMIN_PASSWORD ausentes; se omite la creación del usuario ADMIN inicial.");
            return;
        }

        if (!usuarioRepository.existsByEmail(emailNormalizado)) {
            Usuario admin = Usuario.builder()
                    .nombre("Administrador")
                    .apellido("Sistema")
                    .email(emailNormalizado)
                    .password(passwordEncoder.encode(adminPassword))
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .build();

            usuarioRepository.save(admin);
            log.info("Usuario ADMIN inicial creado exitosamente: {}", emailNormalizado);
        } else {
            log.info("Usuario ADMIN ({}) ya existe en la base de datos.", emailNormalizado);
        }
    }
}
