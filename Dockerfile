ARG BASE_REGISTRY=docker.io/library

# ---------- 编译阶段 ----------
FROM ${BASE_REGISTRY}/eclipse-temurin:11-jdk AS build
WORKDIR /app
COPY src ./src
RUN javac -encoding UTF-8 -d out src/*.java

# ---------- 运行阶段 ----------
FROM ${BASE_REGISTRY}/eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/out ./out

EXPOSE 8080
CMD ["java", "-cp", "out", "Main"]
