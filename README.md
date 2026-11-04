# Backend — Herramienta Digital Guantes (API REST)

> **Proyecto de Grado:** *"Fortalecimiento de competencias técnicas en la postura de guantes quirúrgicos mediante el uso de una herramienta digital interactiva en estudiantes de enfermería"*  
> **Institución:** Universidad Cooperativa de Colombia (UCC) — Campus Pasto  
> **Programa:** Ingeniería de Software  

---

## 1. Pila Tecnológica

- **Lenguaje:** Java 17
- **Framework:** Spring Boot `4.1.1`
- **Seguridad:** Spring Security stateless + JWT (`jjwt 0.12.6`) y control de acceso por roles (`ESTUDIANTE`, `DOCENTE`, `ADMIN`)
- **Persistencia:** Spring Data JPA + Hibernate ORM
- **Base de Datos:** PostgreSQL `18`
- **Documentación OpenAPI / Swagger:** `springdoc-openapi-starter-webmvc-ui` `3.2.0`
- **Despliegue:** Docker multi-etapa (Render en producción / Docker Compose en local)

---

## 2. Variables de Entorno

La aplicación **no define contraseñas ni secretos por defecto** en `application.properties`. Al arrancar valida que `JWT_SECRET` exista y tenga una longitud mínima de **32 bytes (256 bits)**; de lo contrario detiene el inicio con un error explícito.

Copia `.env.example` a `.env` para configurar la ejecución local:

```bash
cp .env.example .env
```

| Variable | Obligatoria | Valor por defecto | Descripción |
| :--- | :---: | :--- | :--- |
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5432/guantes_db` | Cadena JDBC de conexión a PostgreSQL 18. |
| `SPRING_DATASOURCE_USERNAME` | No | `postgres` | Usuario de la base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | **Sí** | *(ninguno)* | Contraseña del usuario de PostgreSQL. |
| `JWT_SECRET` | **Sí** | *(ninguno)* | Clave secreta HMAC-SHA para firmar tokens JWT (**mínimo 32 bytes**). |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173` | Lista de orígenes permitidos separados por coma (en Render: URL de Vercel). No admite `*` porque `allowCredentials=true`. |
| `ADMIN_EMAIL` | Opcional | *(vacío)* | Correo del administrador inicial sembrado por `DataInitializer` si aún no existe. |
| `ADMIN_PASSWORD` | Opcional | *(vacío)* | Contraseña del administrador inicial. Si falta `ADMIN_EMAIL` o `ADMIN_PASSWORD`, no se crea el usuario y se registra una advertencia en el log. |
| `PORT` | No | `8080` | Puerto HTTP de escucha del servidor embebido. |

---

## 3. Perfiles de Configuración

1. **Perfil por defecto (`application.properties`):** Pensado para producción (Render) y contenedores Docker. Lee todas las credenciales exclusivamente desde variables de entorno.
2. **Perfil de desarrollo local (`dev`):**
   - Copia `src/main/resources/application-dev.properties.example` a `src/main/resources/application-dev.properties` (este último está ignorado en `.gitignore`) y ajusta tus credenciales locales.
   - Ejecuta con Maven activando el perfil `dev`:
     ```bash
     ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
     ```
3. **Configuración de pruebas (`src/test/resources/application.properties`):** Provee propiedades aisladas exclusivas para la suite automatizada de modo que `./mvnw test` funcione sin variables de entorno externas.

---

## 4. Ejecución Local con Docker (Un Solo Comando)

El repositorio incluye un archivo `docker-compose.yml` que levanta **PostgreSQL 18** (`postgres:18-alpine` con *healthcheck*) y compila/ejecuta la **API** automáticamente:

```bash
# Opcional: personalizar variables copiando .env.example a .env
cp .env.example .env

# Levantar PostgreSQL 18 + API Spring Boot en http://localhost:8080
docker compose up --build
```

Para detener los servicios conservando los datos del volumen:

```bash
docker compose down
```

---

## 5. Documentación Interactiva (Swagger / OpenAPI)

Con la API en ejecución, la documentación OpenAPI 3 es accesible públicamente sin token:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Especificación JSON:** `http://localhost:8080/v3/api-docs`
- **Especificación YAML:** `http://localhost:8080/v3/api-docs.yaml`

Para probar endpoints protegidos desde Swagger UI:
1. Ejecuta `POST /api/auth/login` y copia el campo `token` de la respuesta.
2. Haz clic en el botón **Authorize** (esquema `bearerAuth` JWT), pega el token y confirma.

---

## 6. Comandos de Prueba

```bash
# En Linux / macOS:
./mvnw test

# En Windows (PowerShell / CMD):
.\mvnw.cmd test
```

La suite verifica:
- Validación de longitud mínima de `JWT_SECRET` (`JwtServiceTest`).
- Restricción de orígenes CORS y rechazo de orígenes no autorizados (`CorsConfigTest`).
- Creación condicional del administrador inicial desde variables de entorno (`DataInitializerTest`).
- Exposición pública de `/v3/api-docs` y `/swagger-ui.html` con esquema `bearerAuth` en Spring Boot 4.1 (`OpenApiIntegrationTest`).
- Servicios y controladores de autenticación, usuarios, intentos y modelos de IA.

---

## 7. Resumen de Endpoints Principales

| Grupo | Ruta Base | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| **Autenticación** | `/api/auth/register`, `/api/auth/login`, `/api/auth/perfil` | Público / Autenticado | Registro, emisión de JWT y consulta de perfil actual. |
| **Intentos de Práctica** | `/api/intentos` | Autenticado (`GET /api/intentos` solo `DOCENTE` y `ADMIN`) | Registro de sesiones, telemetría de IA e historial por estudiante. |
| **Progreso** | `/api/progreso` | Autenticado (`POST /recalcular` solo `DOCENTE` y `ADMIN`) | Métricas acumuladas de aprendizaje, puntajes y nivel alcanzado. |
| **Modelo IA** | `/api/ia` | Autenticado / `ADMIN` | Gestión de versiones del clasificador TensorFlow.js y estadísticas de uso. |
| **Usuarios** | `/api/usuarios` y `/api/admin/usuarios` | `DOCENTE` / `ADMIN` | Consulta y administración integral de cuentas y roles. |
