# DEMO2 · 宠物小岛后端 API

卡通宠物岛项目的 Java 后端，基于 JDK 内置 `HttpServer`，零第三方依赖。

## 架构

```
Main.java        # 服务器启动 + 统一路由分发（线程池 8 线程）
Pet.java         # 宠物模型：饱食度/心情随时间衰减、喂食、玩耍、经验升级
TaskStore.java   # 内存任务看板：增删改查，完成任务奖励宠物经验
Json.java        # 极简 JSON 工具（转义 / 字段提取）
```

## 接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | /api/time | 服务器时间 |
| GET  | /api/health | 健康检查 |
| GET  | /api/pet | 宠物状态 |
| POST | /api/pet/feed | 喂食 `{"food":"蛋糕"}` |
| POST | /api/pet/play | 玩耍（+经验） |
| GET  | /api/tasks | 任务列表 |
| POST | /api/tasks | 新建任务 `{"title":"..."}` |
| POST | /api/tasks/{id}/toggle | 切换完成状态（奖励宠物 20 经验） |
| DELETE | /api/tasks/{id} | 删除任务 |
| GET  | /api/stats | 宠物 + 任务统计 |

## 本地运行

双击 `run.bat`（需 JDK 11+），或：

```bat
javac -encoding UTF-8 -d out src\*.java
java -cp out Main
```

服务监听 `8080` 端口。数据存于内存，重启后清空。

## 部署

- `Dockerfile`：多阶段构建（temurin 11）
- `Jenkinsfile`：构建镜像 → 部署到 `demo-net` 网络（别名 `backend`），供前端 nginx 反代 `/api`

与前端仓库约定：前端 nginx 通过 `http://backend:8080` 反代 `/api/`。
