# ── Stage 1 : Build ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# Télécharger les dépendances en cache séparé (accélère les rebuilds)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q
# Le JAR s'appelle app.jar grâce à <finalName>app</finalName> dans pom.xml

# ── Stage 2 : Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copier le JAR depuis le stage de build
COPY --from=builder /build/target/app.jar app.jar

# Port exposé
EXPOSE 8080

# Lancement exactement comme demandé par le prof
ENTRYPOINT ["java", "-jar", "app.jar"]
