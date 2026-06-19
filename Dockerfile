FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM python:3.11-slim
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jre-headless \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir scikit-learn numpy

COPY --from=builder /build/target/app.jar app.jar
COPY ml/ /app/ml/
RUN mkdir -p /app/ml/models

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
