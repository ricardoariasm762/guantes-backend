# Backend - Herramienta Digital Interactiva para Postura de Guantes Quirúrgicos

**Proyecto de Grado:** "Fortalecimiento de competencias técnicas en la postura de guantes quirúrgicos mediante el uso de una herramienta digital interactiva en estudiantes de enfermería"  
**Institución:** Universidad Cooperativa de Colombia (UCC), Campus Pasto  
**Programa:** Ingeniería de Software  

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Java 17
- **Framework:** Spring Boot 3.2.x
- **Seguridad:** Spring Security con autenticación stateless basada en JWT (JSON Web Tokens)
- **Persistencia:** Spring Data JPA + Hibernate ORM
- **Base de Datos:** PostgreSQL (`guantes_db`)
- **Documentación:** OpenAPI 3 / Swagger (SpringDoc OpenAPI 2.5.x)
- **Productividad:** Project Lombok
- **Construcción y Dependencias:** Apache Maven Wrapper (`./mvnw`)

---

## 🧠 Arquitectura Híbrida IA - Reglas Clínicas

El sistema implementa una arquitectura desacoplada donde el procesamiento de visión computacional e inteligencia artificial se ejecuta íntegramente en el navegador del usuario (**client-side**):

1. **Captura y Extracción:** MediaPipe Hands captura 21 puntos anatómicos (landmarks) por mano y computa 79 características geométricas (distancias euclidianas, ángulos de articulación y ratios).
2. **Inferencia Local:** Un modelo entrenado en TensorFlow.js clasifica la fase o etapa del protocolo quirúrgico sin transmitir video fuera del equipo cliente, garantizando privacidad total.
3. **Mecanismo Híbrido:** Las predicciones de la red neuronal se combinan con reglas clínicas de asepsia.
4. **Backend (Ingesta y Telemetría):** El backend almacena los resultados clínicos junto con la telemetría del modelo para fines de auditoría académica, investigación y mejora continua del algoritmo.

### Clasificación de la Fuente de Decisión (`FuentePrediccion`)

| Fuente | Descripción Clínica / Técnica |
| :--- | :--- |
| `IA` | La decisión provino principalmente del clasificador de Machine Learning (TensorFlow.js). |
| `REGLAS` | Prevalecieron las reglas deterministas de técnica aséptica (inferido automáticamente cuando la confianza es < 0.60). |
| `CONSENSO` | Ambas fuentes (IA y reglas clínicas) coincidieron plenamente (inferido automáticamente cuando la confianza es $\ge$ 0.80). |
| `MIXTO` | Conflicto entre la IA y las reglas resuelto por prioridad heurística (inferido automáticamente cuando la confianza está entre 0.60 y 0.80). |

---

## 🗄️ Modelo de Datos y Esquema

### 1. Entidad `IntentoPractica` (Tabla `intentos_practica`)

Campos de metadatos de IA incorporados:

| Columna | Tipo SQL | Nullable | Descripción |
| :--- | :--- | :---: | :--- |
| `confianza_promedio` | `DOUBLE PRECISION` | Sí | Valor normalizado entre 0.0 y 1.0 de la certeza promedio del modelo durante la práctica. |
| `fuente_prediccion` | `VARCHAR(20)` | Sí | Enum (`IA`, `REGLAS`, `CONSENSO`, `MIXTO`). |
| `version_modelo` | `VARCHAR(50)` | Sí | Identificador de la versión del modelo TF.js empleada (ej: `v1.0-2026-10`). |
| `detalle_ia` | `TEXT` | Sí | JSON serializado con métricas detalladas por landmark, fase y tiempo. |

### 2. Entidad `ModeloIA` (Tabla `modelos_ia`)

Tabla dedicada al control y auditoría de versiones del modelo:

| Columna | Tipo SQL | Restricción | Descripción |
| :--- | :--- | :---: | :--- |
| `id` | `BIGSERIAL` | PRIMARY KEY | Identificador único autoincremental. |
| `version` | `VARCHAR(50)` | UNIQUE, NOT NULL | Tag o código de versión del modelo. |
| `accuracy` | `DOUBLE PRECISION` | NOT NULL | Métrica de exactitud global (entre 0.0 y 1.0). |
| `num_clases` | `INTEGER` | NOT NULL | Cantidad de clases o pasos clasificados. |
| `num_muestras` | `INTEGER` | NOT NULL | Cantidad de muestras de entrenamiento utilizadas. |
| `fecha_entrenamiento` | `TIMESTAMP` | NOT NULL | Fecha y hora de entrenamiento / registro. |
| `descripcion` | `TEXT` | Nullable | Notas técnicas de la arquitectura o hiperparámetros. |
| `activo` | `BOOLEAN` | Default `false` | Indica si es la versión activa para los clientes frontend. |

---

## 🔄 Estrategia de Migración de Base de Datos

### Modo Automático (Desarrollo)
La aplicación cuenta con:
```properties
spring.jpa.hibernate.ddl-auto=update
```
Hibernate alterará la tabla `intentos_practica` para añadir las columnas nullable y creará la nueva tabla `modelos_ia` al iniciar la aplicación.

### Modo Versionado (Producción / Alternativa Flyway o Liquibase)
Si se desea realizar la migración mediante scripts SQL versionados en PostgreSQL:

```sql
-- 1. Ampliar intentos_practica con campos de telemetría de IA
ALTER TABLE intentos_practica
    ADD COLUMN IF NOT EXISTS confianza_promedio DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS fuente_prediccion VARCHAR(20),
    ADD COLUMN IF NOT EXISTS version_modelo VARCHAR(50),
    ADD COLUMN IF NOT EXISTS detalle_ia TEXT;

-- 2. Crear tabla de versiones de modelo
CREATE TABLE IF NOT EXISTS modelos_ia (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL UNIQUE,
    accuracy DOUBLE PRECISION NOT NULL,
    num_clases INTEGER NOT NULL,
    num_muestras INTEGER NOT NULL,
    fecha_entrenamiento TIMESTAMP NOT NULL,
    descripcion TEXT,
    activo BOOLEAN DEFAULT FALSE
);
```

---

## 👤 Usuario Administrador Inicial

La clase `DataInitializer` verifica la existencia del usuario administrador al arrancar. Si no existe, lo crea automáticamente:

- **Email:** `admin@ucc.edu.co`
- **Contraseña temporal:** `admin123456`
- **Rol:** `ADMIN` (`ROLE_ADMIN`)

> ⚠️ **Importante:** Cambiar la contraseña en el primer inicio en entornos de producción.

---

## 🚀 Catálogo de Endpoints de Inteligencia Artificial

Todos los endpoints protegidos requieren el encabezado `Authorization: Bearer <TOKEN_JWT>`.

| Método | Endpoint | Rol Requerido | Descripción |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/ia/version` | `ADMIN` | Registra una nueva versión de modelo y gestiona la activación atómica. |
| `GET` | `/api/ia/version/activa` | Autenticado | Retorna la versión del modelo activa para sincronización del frontend. |
| `GET` | `/api/ia/versiones` | `ADMIN` | Lista todas las versiones de modelos registradas. |
| `GET` | `/api/ia/estadisticas` | Autenticado | Obtiene métricas globales de adopción, confianza y volumen de IA. |

---

## 📄 Ejemplos de Petición y Respuesta

### 1. Registrar Intento con Metadatos de IA

**`POST /api/intentos`**

```json
{
  "modoPractica": "GUIADO",
  "puntaje": 96.5,
  "duracionSegundos": 115,
  "completado": true,
  "observaciones": "Excelente técnica de tracción de manga quirúrgica",
  "confianzaPromedio": 0.88,
  "fuentePrediccion": "CONSENSO",
  "versionModelo": "v1.0-2026-10",
  "detalleIA": "{\"detecciones\":[{\"paso\":1,\"confianza\":0.92},{\"paso\":2,\"confianza\":0.84}]}",
  "pasos": [
    {
      "numeroPaso": 1,
      "nombrePaso": "Apertura del paquete estéril",
      "descripcion": "Verificación de esterilidad y apertura sin contaminación",
      "exitoso": true,
      "tiempoSegundos": 20,
      "precision": 0.95
    }
  ]
}
```

**Respuesta (`201 CREATED`):**

```json
{
  "id": 42,
  "usuarioId": 5,
  "usuarioNombre": "Carlos Mendoza",
  "usuarioEmail": "carlos.mendoza@campusucc.edu.co",
  "modoPractica": "GUIADO",
  "fechaInicio": "2026-10-12T16:20:00",
  "fechaFin": "2026-10-12T16:21:55",
  "puntaje": 96.5,
  "duracionSegundos": 115,
  "completado": true,
  "observaciones": "Excelente técnica de tracción de manga quirúrgica",
  "confianzaPromedio": 0.88,
  "fuentePrediccion": "CONSENSO",
  "versionModelo": "v1.0-2026-10",
  "detalleIA": "{\"detecciones\":[{\"paso\":1,\"confianza\":0.92},{\"paso\":2,\"confianza\":0.84}]}",
  "pasos": [
    {
      "id": 180,
      "numeroPaso": 1,
      "nombrePaso": "Apertura del paquete estéril",
      "descripcion": "Verificación de esterilidad y apertura sin contaminación",
      "exitoso": true,
      "tiempoSegundos": 20,
      "precision": 0.95,
      "observacionesError": null,
      "fechaHora": "2026-10-12T16:20:20"
    }
  ]
}
```

---

### 2. Registrar Versión de Modelo (Rol ADMIN)

**`POST /api/ia/version`**

```json
{
  "version": "v1.0-2026-10",
  "accuracy": 0.945,
  "numClases": 6,
  "numMuestras": 15200,
  "descripcion": "Modelo TF.js entrenado con arquitectura densa sobre 79 features de landmarks de MediaPipe Hands",
  "activo": true
}
```

**Respuesta (`201 CREATED`):**

```json
{
  "id": 1,
  "version": "v1.0-2026-10",
  "accuracy": 0.945,
  "numClases": 6,
  "numMuestras": 15200,
  "fechaEntrenamiento": "2026-10-12T16:15:30",
  "descripcion": "Modelo TF.js entrenado con arquitectura densa sobre 79 features de landmarks de MediaPipe Hands",
  "activo": true
}
```

---

### 3. Consultar Versión Activa

**`GET /api/ia/version/activa`**

**Respuesta (`200 OK`):**

```json
{
  "id": 1,
  "version": "v1.0-2026-10",
  "accuracy": 0.945,
  "numClases": 6,
  "numMuestras": 15200,
  "fechaEntrenamiento": "2026-10-12T16:15:30",
  "descripcion": "Modelo TF.js entrenado con arquitectura densa sobre 79 features de landmarks de MediaPipe Hands",
  "activo": true
}
```

---

### 4. Consultar Estadísticas Agregadas de IA

**`GET /api/ia/estadisticas`**

**Respuesta (`200 OK`):**

```json
{
  "confianzaPromedioGlobal": 0.865,
  "totalIntentosConIA": 142,
  "totalIntentosTotales": 180,
  "tasaUsoIA": 0.789
}
```

---

## 🧪 Pruebas Automatizadas

Para compilar y ejecutar la suite completa de pruebas:

```bash
# Compilar proyecto
./mvnw clean compile

# Ejecutar pruebas unitarias y de integración
./mvnw test
```

Resultados incluidos en la suite de pruebas:
- `ModeloIAServiceTest`: Pruebas del ciclo de vida del modelo, unicidad de versión y estadísticas agregadas.
- `IntentoServiceTest`: Pruebas de persistencia de intentos, compatibilidad hacia atrás y reglas heurísticas automáticas de `FuentePrediccion`.
- `ModeloIAControllerTest`: Pruebas de integración HTTP MockMvc con verificación de esquemas, códigos de respuesta (200, 201, 400, 404) y restricciones de seguridad `@PreAuthorize`.

---

## 📚 Documentación Interactiva (Swagger / OpenAPI)

Con el backend en ejecución, accede a la interfaz gráfica interactiva de Swagger:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Especificación OpenAPI (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
