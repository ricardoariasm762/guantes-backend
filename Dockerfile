# ==========================================
# Etapa 1: Compilación con Maven y JDK 17
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar descriptor de dependencias y código fuente
COPY pom.xml .
COPY src ./src

# Compilar empaquetado optimizado omitiendo pruebas para agilizar el despliegue
RUN mvn clean package -DskipTests

# ==========================================
# Etapa 2: Entorno de Ejecución Ligero JRE 17
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar el artefacto .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Puerto de servicio asignado
EXPOSE 8080

# Comando de inicio del servidor
ENTRYPOINT ["java", "-jar", "app.jar"]
