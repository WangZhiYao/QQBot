# 构建层
FROM gradle:9.7-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle :sample-app:shadowJar --no-daemon

# 运行层
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/sample-app/build/libs/*-all.jar app.jar
RUN mkdir -p /app/data
EXPOSE 8080
ENV QQ_APP_ID=""
ENV QQ_APP_SECRET=""
VOLUME ["/app/data"]
ENTRYPOINT ["java", "-jar", "app.jar"]
