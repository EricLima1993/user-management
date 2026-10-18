# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

# O cache do Maven fica fora da imagem, então rebuilds não baixam tudo de novo.
# Os testes rodam à parte (./mvnw verify); aqui só geramos o .jar.
# chmod: no Windows o Git não guarda a permissão de execução do mvnw.
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw -B -q -DskipTests package

# ---------- Etapa 2: executar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuário sem privilégios: se alguém invadir a aplicação, não é root no container.
RUN useradd --system --no-create-home appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# MaxRAMPercentage faz a JVM respeitar o limite de memória do container.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
