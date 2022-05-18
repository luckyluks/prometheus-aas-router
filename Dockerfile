FROM openjdk:8-jdk-alpine
RUN apk add --no-cache bash
RUN mkdir /app
WORKDIR /app
COPY target/prometheus-aas-router-1.0-SNAPSHOT-jar-with-dependencies.jar /app
COPY script/* /app/
RUN chmod +x /app/wait-for-it.sh /app/docker-entrypoint.sh
ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["java", "-jar", "prometheus-aas-router-1.0-SNAPSHOT-jar-with-dependencies.jar"]
# CMD ["sh", "-c", "tail -f /dev/null"]