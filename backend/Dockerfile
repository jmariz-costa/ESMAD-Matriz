# syntax=docker/dockerfile:1
# Imagem do Backend da Matriz (Ficha PL 05, Exercício 2).
# Construir a partir da raiz do repositório:  docker build -t matriz-backend backend
# Não leva credenciais: as variáveis (backend/.env.example) são passadas no arranque.

# --- 1. Compilação: JDK 21 e o Maven Wrapper do projeto (a mesma versão do Maven que em dev) ---
FROM eclipse-temurin:21.0.13_9-jdk-noble AS build
WORKDIR /build

# Sem unzip, o mvnw descarrega o .tar.gz do Maven e a soma SHA-256 (que é a do .zip) falha
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*

# Dependências numa camada própria: só voltam a ser descarregadas quando o pom.xml muda
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

# Os testes não correm aqui: precisam de Docker (Dev Services) e correm na pipeline (job backend-tests)
COPY src src
RUN ./mvnw -B -q package -DskipTests

# --- 2. Execução: só o JRE 21 e a aplicação já compilada ---
FROM eclipse-temurin:21.0.13_9-jre-noble
WORKDIR /app

# Utilizador sem privilégios; os ficheiros da aplicação ficam do root, só para leitura
RUN useradd --uid 10001 --no-create-home --shell /usr/sbin/nologin matriz
COPY --from=build /build/target/quarkus-app/ ./
USER 10001

EXPOSE 8081
CMD ["java", "-jar", "quarkus-run.jar"]
