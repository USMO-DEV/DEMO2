ARG BASE_REGISTRY=docker.io/library

# ---------- 编译阶段 ----------
FROM ${BASE_REGISTRY}/eclipse-temurin:11-jdk AS build
WORKDIR /app
COPY src/Main.java ./
RUN javac -encoding UTF-8 Main.java

# ---------- 运行阶段 ----------
FROM ${BASE_REGISTRY}/eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/*.class ./

EXPOSE 8080
CMD ["java", "Main"]
