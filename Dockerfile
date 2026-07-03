# Use official Java 21 runtime as base image
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Copy Maven files
COPY pom.xml .mvn/ .

# Copy source code
COPY src/ src/

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /workspace/target/remail-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
