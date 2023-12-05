FROM maven:3-eclipse-temurin-17-alpine AS builder
WORKDIR /application
COPY . .
RUN mvn package
RUN java -Djarmode=layertools -jar target/*.jar extract

FROM eclipse-temurin:17-jre-alpine

EXPOSE 8080

WORKDIR application
COPY --from=builder application/dependencies/  ./
COPY --from=builder application/spring-boot-loader/  ./
COPY --from=builder application/snapshot-dependencies/  ./
COPY --from=builder application/application/  ./

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]