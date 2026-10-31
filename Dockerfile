# ===== 1) Construir el .jar con Maven =====
FROM maven:3.9-eclipse-temurin-17 AS construir
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

# ===== 2) Ejecutar =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=construir /build/target/*.jar app.jar
# Consultas del modo solucion (la app las lee al arrancar)
COPY soluciones/etapa3_logica/consultas_app.sql soluciones/etapa3_logica/consultas_app.sql
# Script de arranque (prepara el wallet de Oracle). sed quita saltos de linea de Windows si los hubiera.
COPY entrypoint.sh /app/entrypoint.sh
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

# Hora de Peru y ajustes para el plan gratuito (512 MB de RAM)
ENV TZ=America/Lima \
    APP_COMPLETAR_CON_SOLUCION=true \
    SPRING_THYMELEAF_CACHE=true \
    LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_JDBC_CORE=INFO \
    SERVER_TOMCAT_THREADS_MAX=20

EXPOSE 8080
ENTRYPOINT ["sh", "/app/entrypoint.sh"]
