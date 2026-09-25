FROM eclipse-temurin:21-jdk-alpine

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/

EXPOSE 8080

CMD ["./mvnw", "-B", "spring-boot:run"]
