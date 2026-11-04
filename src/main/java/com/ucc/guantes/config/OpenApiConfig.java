package com.ucc.guantes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("API Herramienta Digital Guantes - UCC")
                        .version("1.0.0")
                        .description("API REST para el simulador web de práctica y evaluación de postura de guantes quirúrgicos estériles - Universidad Cooperativa de Colombia, Campus Pasto")
                        .contact(new Contact()
                                .name("Proyecto de Grado Ingeniería de Software - UCC Pasto")
                                .email("soporte.guantes@ucc.edu.co"))
                        .license(new License()
                                .name("Todos los derechos reservados – Universidad Cooperativa de Colombia y el autor, 2026")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el token JWT obtenido en /api/auth/login")));
    }
}
