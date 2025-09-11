# 멀티스테이지 빌드를 사용한 최적화된 Dockerfile
FROM gradle:8.10.2-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# 비루트 사용자 생성
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 권한 설정
RUN chown -R spring:spring /app
USER spring

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]