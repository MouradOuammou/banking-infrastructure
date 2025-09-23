FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests

# Create final image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy jar file
COPY --from=0 /app/target/transactions-service-1.0.0.jar app.jar

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
