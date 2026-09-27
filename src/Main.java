import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

/**
 * 卡通宠物岛后端 API（前后端分离版）
 *
 * 接口一览：
 *   GET    /api/time            服务器时间
 *   GET    /api/health          健康检查
 *   GET    /api/pet             宠物状态
 *   POST   /api/pet/feed        喂食 {"food":"蛋糕"}
 *   POST   /api/pet/play        玩耍
 *   GET    /api/tasks           任务列表
 *   POST   /api/tasks           新建任务 {"title":"..."}
 *   POST   /api/tasks/{id}/toggle   切换任务完成状态
 *   DELETE /api/tasks/{id}       删除任务
 *   GET    /api/stats           宠物 + 任务统计
 */
public class Main {

    private static final int PORT = 8080;
    private static final Pet PET = new Pet();
    private static final TaskStore TASKS = new TaskStore(PET);

    public static void main(String[] args) throws IOException {
        // 支持自定义端口：java -cp out Main 8081
        int port = PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/", Main::route);
        server.start();
        System.out.println("宠物岛后端已启动: http://localhost:" + port);
        System.out.println("接口示例: GET /api/pet  POST /api/pet/feed  GET /api/tasks  GET /api/stats");
    }

    /** 统一路由分发 */
    private static void route(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            dispatch(ex, path, method);
        } catch (NumberFormatException e) {
            send(ex, 400, "{\"error\":\"任务 id 必须是数字\"}", "application/json; charset=utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"服务器开小差了\"}", "application/json; charset=utf-8");
        }
    }

    private static void dispatch(HttpExchange ex, String path, String method) throws IOException {
        // ---- 基础 ----
        if (path.equals("/api/time")) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            send(ex, 200, "{\"time\":\"" + time + "\"}", "application/json; charset=utf-8");
        } else if (path.equals("/api/health")) {
            send(ex, 200, "{\"status\":\"UP\",\"service\":\"pet-island-api\"}",
                    "application/json; charset=utf-8");
        }
        // ---- 宠物 ----
        else if (path.equals("/api/pet")) {
            send(ex, 200, PET.toJson(), "application/json; charset=utf-8");
        } else if (path.equals("/api/pet/feed") && method.equals("POST")) {
            String body = readBody(ex);
            String msg = PET.feed(Json.get(body, "food"));
            send(ex, 200, "{\"message\":\"" + Json.esc(msg) + "\",\"pet\":" + PET.toJson() + "}",
                    "application/json; charset=utf-8");
        } else if (path.equals("/api/pet/play") && method.equals("POST")) {
            String msg = PET.play();
            send(ex, 200, "{\"message\":\"" + Json.esc(msg) + "\",\"pet\":" + PET.toJson() + "}",
                    "application/json; charset=utf-8");
        }
        // ---- 任务 ----
        else if (path.equals("/api/tasks") && method.equals("GET")) {
            send(ex, 200, "[" + String.join(",", TASKS.list()) + "]",
                    "application/json; charset=utf-8");
        } else if (path.equals("/api/tasks") && method.equals("POST")) {
            String title = Json.get(readBody(ex), "title").trim();
            if (title.isEmpty()) {
                send(ex, 400, "{\"error\":\"任务标题不能为空\"}", "application/json; charset=utf-8");
                return;
            }
            TaskStore.Task t = TASKS.add(title);
            send(ex, 200, "{\"message\":\"新任务已挂上看板！\",\"task\":" + t.toJson() + "}",
                    "application/json; charset=utf-8");
        } else if (path.startsWith("/api/tasks/") && path.endsWith("/toggle")
                && method.equals("POST")) {
            long id = Long.parseLong(path.split("/")[3]);
            String msg = TASKS.toggle(id);
            if (msg == null) {
                send(ex, 404, "{\"error\":\"任务不存在\"}", "application/json; charset=utf-8");
            } else {
                send(ex, 200, "{\"message\":\"" + Json.esc(msg) + "\"}", "application/json; charset=utf-8");
            }
        } else if (path.startsWith("/api/tasks/") && method.equals("DELETE")) {
            long id = Long.parseLong(path.split("/")[3]);
            if (TASKS.delete(id)) {
                send(ex, 200, "{\"message\":\"任务已从看板撕掉！\"}", "application/json; charset=utf-8");
            } else {
                send(ex, 404, "{\"error\":\"任务不存在\"}", "application/json; charset=utf-8");
            }
        }
        // ---- 统计 ----
        else if (path.equals("/api/stats")) {
            send(ex, 200, "{\"pet\":" + PET.toJson() + ",\"tasks\":" + TASKS.statsJson() + "}",
                    "application/json; charset=utf-8");
        }
        // ---- 根路径 ----
        else if (path.equals("/")) {
            send(ex, 200, "宠物岛后端 API 已运行！接口: /api/time /api/pet /api/pet/feed "
                    + "/api/pet/play /api/tasks /api/stats", "text/plain; charset=utf-8");
        } else {
            send(ex, 404, "{\"error\":\"接口不存在\"}", "application/json; charset=utf-8");
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void send(HttpExchange ex, int code, String body, String type) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
