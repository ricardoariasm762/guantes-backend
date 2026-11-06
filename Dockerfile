# ==========================================
# Etapa 1: Compilación con Maven y JDK 17
# ==========================================
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar descriptor de dependencias y código fuente
COPY pom.xml .
COPY src ./src

# Compilar empaquetado optimizado omitiendo pruebas para agilizar la construcción
RUN mvn clean package -DskipTests

# ==========================================
# Etapa 2: Entorno de Ejecución Ligero JRE 17
# ==========================================
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
WORKDIR /app

# Configuración de zona horaria oficial para Colombia
ENV TZ=America/Bogota

# Instalar curl para el HEALTHCHECK y crear usuario y grupo sin privilegios
RUN apk add --no-cache curl \
    && addgroup -S appgroup \
    && adduser -S appuser -G appgroup

# Copiar el artefacto .jar generado en la etapa anterior asignando permisos al usuario no root
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# Ejecutar con usuario sin privilegios
USER appuser:appgroup

# Puerto de servicio
EXPOSE 8080

# Verificación de salud periódica contra el endpoint de Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Comando de inicio del servidor con límite de memoria adaptativo (-XX:MaxRAMPercentage=75)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
