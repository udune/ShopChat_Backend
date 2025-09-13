FROM eclipse-temurin:17-jre-alpine

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

WORKDIR /app
COPY app.jar /app/app.jar

EXPOSE 8080

# Cloud Run에 최적화된 JVM 옵션 (메모리 여유 확보)
ENV JAVA_OPTS="-Xmx4g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -Djava.security.egd=file:/dev/./urandom -Dspring.jmx.enabled=false -Dspring.main.banner-mode=off"

# Spring Boot가 포트를 제대로 바인딩하도록 명시적으로 설정
ENTRYPOINT ["sh", "-c", "echo '=== FeedShop Starting ===' && echo 'JAVA_OPTS: ' $JAVA_OPTS && echo 'PORT: ' $PORT && exec java $JAVA_OPTS -jar app.jar"]