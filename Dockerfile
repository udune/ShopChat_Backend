# 빠른 시작을 위한 최적화된 Dockerfile
FROM eclipse-temurin:17-jre-alpine

# 필수 패키지만 설치
RUN apk add --no-cache curl

WORKDIR /app

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# JVM 최적화 (시작 속도 우선)
ENV JAVA_OPTS="-Xmx6g -Xms2g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -Djava.security.egd=file:/dev/./urandom -Dspring.jmx.enabled=false -Dspring.main.banner-mode=off"

# Spring Boot 시작 명령어 (로그 포함)
ENTRYPOINT ["sh", "-c", "echo '=== FeedShop Starting ===' && echo 'Memory: ' && free -h && echo 'Java Version: ' && java -version && echo 'JAVA_OPTS: ' $JAVA_OPTS && echo 'PORT: ' $PORT && echo 'Starting application...' && exec java $JAVA_OPTS -jar app.jar"]