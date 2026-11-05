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
- **Métricas y Monitoreo:** `spring-boot-starter-actuator` (`/actuator/health`)
- **Despliegue:** Docker multi-etapa en **Amazon Web Services (AWS)** sobre EC2 `t3.small` (Amazon Linux 2023), Amazon RDS PostgreSQL 18, Amazon ECR y CloudFront / Docker Compose en desarrollo local.

---

## 2. Variables de Entorno

La aplicación **no define contraseñas ni secretos por defecto** en `application.properties`. Al arrancar valida que `JWT_SECRET` exista y tenga una longitud mínima de **32 bytes (256 bits)**; de lo contrario detiene el inicio con un error explícito.

Copia `.env.example` a `.env` para configurar la ejecución local:

```bash
cp .env.example .env
```

| Variable | Obligatoria | Valor por defecto | Descripción |
| :--- | :---: | :--- | :--- |
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5432/guantes_db` | Cadena JDBC de conexión a PostgreSQL 18 (en RDS: `sslmode=require`). |
| `SPRING_DATASOURCE_USERNAME` | No | `postgres` | Usuario de la base de datos (`postgres` por defecto en RDS). |
| `SPRING_DATASOURCE_PASSWORD` | **Sí** | *(ninguno)* | Contraseña del usuario de PostgreSQL (gestionada en SSM Parameter Store). |
| `JWT_SECRET` | **Sí** | *(ninguno)* | Clave secreta HMAC-SHA para firmar tokens JWT (**mínimo 32 bytes**). |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173` | Lista de orígenes permitidos separados por coma (en AWS: URL de CloudFront `https://<distribucion>.cloudfront.net`). No admite `*` porque `allowCredentials=true`. |
| `ADMIN_EMAIL` | Opcional | *(vacío)* | Correo del administrador inicial sembrado por `DataInitializer` si aún no existe. |
| `ADMIN_PASSWORD` | Opcional | *(vacío)* | Contraseña del administrador inicial. Si falta `ADMIN_EMAIL` o `ADMIN_PASSWORD`, no se crea el usuario y se registra una advertencia en el log. |
| `PORT` | No | `8080` | Puerto HTTP de escucha del servidor embebido. |

---

## 3. Perfiles de Configuración

1. **Perfil por defecto (`application.properties`):** Base común agnóstica del entorno. Lee credenciales exclusivamente desde variables de entorno.
2. **Perfil de producción (`application-prod.properties` — activo en AWS con `SPRING_PROFILES_ACTIVE=prod`):**
   - Habilita `server.forward-headers-strategy=framework` para respetar las cabeceras `X-Forwarded-*` remitidas por CloudFront.
   - Configura el pool de conexiones HikariCP con `maximum-pool-size=5` y `minimum-idle=2` para operar eficientemente sobre `db.t4g.micro`.
   - Mantiene `spring.jpa.hibernate.ddl-auto=update` para crear automáticamente las tablas necesarias al arrancar por primera vez en una base de datos nueva de Amazon RDS.
   - Expone únicamente el indicador de salud `/actuator/health` sin mostrar detalles de componentes (`never`).
3. **Perfil de desarrollo local (`dev`):**
   - Copia `src/main/resources/application-dev.properties.example` a `src/main/resources/application-dev.properties` (este último está ignorado en `.gitignore`) y ajusta tus credenciales locales.
   - Ejecuta con Maven activando el perfil `dev`:
     ```bash
     ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
     ```
4. **Configuración de pruebas (`src/test/resources/application.properties`):** Provee propiedades aisladas con base de datos H2 en memoria exclusivas para la suite automatizada de modo que `./mvnw test` funcione sin variables de entorno externas.

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

---

## 8. Despliegue en AWS (EC2 + RDS + ECR + CloudFront)

La API REST se despliega en **Amazon Web Services (AWS)** bajo una arquitectura segura, reproducible y de costo mínimo:

- **Instancia EC2 `t3.small` (Amazon Linux 2023):** Ejecuta el contenedor Docker de Spring Boot bajo el servicio systemd `guantes-backend.service`.
  - **Seguridad perimetral:** El puerto `8080` está restringido estrictamente al rango de IP de CloudFront (`com.amazonaws.global.cloudfront.origin-facing`).
  - **Sin puerto 22 SSH expuesto:** Toda la administración, despliegue y lectura de logs se efectúa mediante **AWS Systems Manager (SSM) Session Manager**.
- **Amazon RDS PostgreSQL 18 (`db.t4g.micro`):** Instancia de base de datos relacional privada (`PubliclyAccessible=false`), con almacenamiento cifrado KMS (20 GB gp3) y copias de seguridad automatizadas por 7 días. Su grupo de seguridad restringe el puerto `5432` exclusivamente al Security Group de la instancia EC2.
- **Amazon ECR (`guantes-backend`):** Registro privado de imágenes Docker con escaneo automatizado de vulnerabilidades en push y política de retención de las últimas 10 imágenes.
- **AWS Systems Manager Parameter Store:** Almacenamiento seguro (`SecureString` KMS) de `JWT_SECRET`, contraseñas y configuración de CORS.

Para el procedimiento completo de despliegue, ejecución de scripts de automatización y resolución de dudas para el jurado, consulta:
👉 **[Guía Integral de Despliegue en AWS (DESPLIEGUE_AWS.md)](file:///c:/Proyecto_grado/Proyectos_Guantes/docs/despliegue/DESPLIEGUE_AWS.md)**.

Los scripts de automatización se ubican en `infra/aws/scripts/`:
- `crear-secretos.ps1` / `.sh`: Inicializa los parámetros en SSM Parameter Store.
- `desplegar-infra-backend.ps1` / `.sh`: Despliega la plantilla CloudFormation `backend.yaml`.
- `publicar-backend.ps1` / `.sh`: Compila la imagen Docker, la publica en ECR y reinicia el servicio en EC2.
- `logs-backend.ps1` / `.sh`: Transmite los logs de Spring Boot en tiempo real a través de SSM.

---

## 9. Estructura de Carpetas

```text
guantes-backend/
├── .github/
│   └── workflows/                # Flujo de despliegue continuo (deploy.yml con OIDC)
├── infra/
│   └── aws/
│       ├── backend.yaml          # Infraestructura como Código (ECR + EC2 + RDS + IAM + SG)
│       ├── github-oidc.yaml      # Configuración de IAM OIDC para GitHub Actions
│       └── scripts/              # Scripts de automatización (PowerShell y Bash)
├── src/
│   ├── main/
│   │   ├── java/com/ucc/guantes/ # Código fuente (controladores, servicios, modelos, seguridad)
│   │   └── resources/
│   │       ├── application.properties      # Configuración común
│   │       └── application-prod.properties # Configuración de producción para AWS
│   └── test/
│       └── java/com/ucc/guantes/ # Suite de pruebas automatizadas (Maven / JUnit 5)
├── Dockerfile                    # Construcción multi-etapa JRE 17 con non-root user y healthcheck
├── docker-compose.yml            # Entorno local PostgreSQL 18 + API
└── pom.xml                       # Dependencias del proyecto Maven (Spring Boot 4.1.1)
```

