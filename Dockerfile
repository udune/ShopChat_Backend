# 문제 해결을 위한 간소화된 Dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 디버깅을 위한 도구 설치
RUN apk add --no-cache curl netcat-openbsd

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 환경 변수 설정 (기본값)
ENV JAVA_OPTS="-Xmx6g -Xms2g -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# 헬스체크 추가 (더 관대한 설정)
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || curl -f http://localhost:8080/ || exit 1

# 시작 스크립트 생성
RUN echo '#!/bin/sh' > /app/start.sh && \
    echo 'echo "=== Starting FeedShop Application ===" ' >> /app/start.sh && \
    echo 'echo "JAVA_OPTS: $JAVA_OPTS"' >> /app/start.sh && \
    echo 'echo "SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"' >> /app/start.sh && \
    echo 'echo "SERVER_PORT: $SERVER_PORT"' >> /app/start.sh && \
    echo 'echo "PORT (Cloud Run): $PORT"' >> /app/start.sh && \
    echo 'echo "Starting application on port 8080..."' >> /app/start.sh && \
    echo 'exec java $JAVA_OPTS -jar app.jar' >> /app/start.sh && \
    chmod +x /app/start.sh

# 시작 명령어
ENTRYPOINT ["/app/start.sh"]