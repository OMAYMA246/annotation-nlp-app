FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache python3 py3-pip && \
    pip3 install --break-system-packages scikit-learn numpy
COPY --from=builder /build/target/app.jar app.jar
COPY ml/ /app/ml/
RUN mkdir -p /app/ml/models
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
