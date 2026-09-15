# ===== 后端Docker构建文件 =====
# 基于Java 17 + Maven构建Spring Boot应用

# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建上传目录
RUN mkdir -p /var/upload/campus-trade

# 复制构建产物
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
