# ---------- 构建阶段：编译打包 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先复制 pom.xml 单独下载依赖，利用 Docker 层缓存加速后续构建
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段：只保留 JRE 与 jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8"

COPY --from=build /app/target/dongchedi.jar /app/dongchedi.jar

# 数据库连接信息由 docker-compose 通过环境变量注入
ENV SPRING_PROFILES_ACTIVE=mysql

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fs http://localhost:8080/api/system/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/dongchedi.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
