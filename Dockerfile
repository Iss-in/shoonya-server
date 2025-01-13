FROM maven:3.9.9-eclipse-temurin-23 AS builder
COPY . .

RUN mvn clean
RUN mvn install:install-file -Dfile=libs/NorenApi-java-2.2.0.jar \
   -DgroupId=com.noren.javaapi \
   -DartifactId=NorenApi-java  \
   -Dversion=2.2.0 \
   -Dpackaging=jar \
   -DgeneratePom=true
RUN mvn package  -Dmaven.test.skip

## RUN mvn dependency:go-offline -B
#CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'"]

#
#FROM eclipse-temurin:23-jdk-alpine
#COPY  target/*.jar app.jar

ENTRYPOINT ["java","-jar","target/trade-server-0.0.1-SNAPSHOT.jar"]

# FROM eclipse-temurin:23-jdk-alpine
# WORKDIR /app
# COPY . .
# # RUN ./mvnw dependency:go-offline -B
# CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'"]
