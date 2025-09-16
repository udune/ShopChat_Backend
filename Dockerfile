FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app
COPY app.jar app.jar
COPY application.properties application.properties

EXPOSE 8080

ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.config.location=file:/app/application.properties -jar app.jar"]