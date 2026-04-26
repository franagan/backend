# Etapa de construcción (Build)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de ejecución (Runtime)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Configuración de variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=prod
ENV MONGODB_URI=mongodb://mongodb:27017/inversion_libre_db

# Exponer el puerto de Spring Boot
EXPOSE 8080

# Comando para ejecutar la aplicación con los permisos de apertura de módulos necesarios
ENTRYPOINT ["java", \
    "--add-opens", "java.base/java.math=ALL-UNNAMED", \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "--add-opens", "java.base/java.util=ALL-UNNAMED", \
    "-jar", "app.jar"]
