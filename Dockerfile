# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy the maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Ensure mvnw has execute permissions
RUN chmod +x mvnw

# Download dependencies (this step is cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline

# Copy the source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the packaged jar from the builder stage
COPY --from=builder /app/target/github-repo-tracker-0.0.1-SNAPSHOT.jar app.jar

# Run the application
# We use the PORT environment variable injected by Render
ENTRYPOINT ["java", "-jar", "app.jar"]
