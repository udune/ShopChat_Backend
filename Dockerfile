# 시작 문제 해결을 위한 최적화된 Dockerfile
FROM eclipse-temurin:17-jre-alpine

# 필요한 패키지 설치
RUN apk add --no-cache \
    curl \
    netcat-openbsd \
    && rm -rf /var/cache/apk/*

WORKDIR /app

# 비루트 사용자 생성
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 권한 설정
RUN chown -R spring:spring /app

# 환경 변수 설정
ENV JAVA_OPTS="-Xmx6g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Dspring.jmx.enabled=false"
ENV SPRING_PROFILES_ACTIVE=prod

USER spring

# 포트 노출
EXPOSE 8080

# 헬스체크 설정 (더 관대한 설정)
HEALTHCHECK --interval=30s --timeout=30s --start-period=300s --retries=10 \
  CMD curl -f http://localhost:8080/ || curl -f http://localhost:8080/actuator/health || exit 1

# 시작 명령어 (디버깅 정보 포함)
ENTRYPOINT ["sh", "-c", "echo '=== FeedShop 시작 ===' && echo 'JAVA_OPTS:' $JAVA_OPTS && echo 'SPRING_PROFILES_ACTIVE:' $SPRING_PROFILES_ACTIVE && echo 'PORT:' $PORT && exec java $JAVA_OPTS -jar app.jar"]