# 빌드 단계: JDK로 실행 가능한 jar를 만든다
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 실행 단계: JRE만 담고 root가 아닌 사용자로 실행한다
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 safetyall
WORKDIR /app
COPY --from=build /workspace/build/libs/safetyall-backend-*.jar app.jar
USER safetyall
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
