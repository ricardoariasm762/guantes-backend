package com.ucc.guantes;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class GuantesBackendApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(
                System.getenv().getOrDefault("APP_ZONA_HORARIA", "America/Bogota")));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(
                System.getenv().getOrDefault("APP_ZONA_HORARIA", "America/Bogota")));
        SpringApplication.run(GuantesBackendApplication.class, args);
    }

}
