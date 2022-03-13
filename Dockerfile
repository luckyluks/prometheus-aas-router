FROM openjdk:8-jdk-alpine

RUN mkdir /app
WORKDIR /app
COPY target/prometheus-aas-router-1.0-SNAPSHOT-jar-with-dependencies.jar /app
CMD ["java", "-jar", "prometheus-aas-router-1.0-SNAPSHOT-jar-with-dependencies.jar"]
