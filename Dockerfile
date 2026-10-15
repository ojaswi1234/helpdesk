# Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S helpdeskgroup && adduser -S helpdesk -G helpdeskgroup
RUN mkdir -p /app/uploads/attachments && chown -R helpdesk:helpdeskgroup /app/uploads

COPY --from=builder --chown=helpdesk:helpdeskgroup /app/target/helpdesk-pro-app.jar app.jar

USER helpdesk:helpdeskgroup

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
