FROM maven:3.9.9-eclipse-temurin-23 AS builder
COPY . .
RUN mvn clean package
## RUN mvn dependency:go-offline -B
#CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'"]

#
FROM eclipse-temurin:23-jdk-alpine
COPY  target/*.jar app.jar
ENTRYPOINT ["java","-jar","-Dspring.config.location=src/main/resources/application.yaml"," app.jar"]

# FROM eclipse-temurin:23-jdk-alpine
# WORKDIR /app
# COPY . .
# # RUN ./mvnw dependency:go-offline -B
# CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'"]
