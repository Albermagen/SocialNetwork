# Imagen de la app para Compose/CI. En desarrollo normal no se usa (mvn spring-boot:run).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY .mvn .mvn
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp package -DskipTests
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
