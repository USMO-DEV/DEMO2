import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 后端 API 服务（前后端分离版）
 * 只提供 /api/time 接口，不再托管静态页面。
 * 静态页面由 frontend 的 nginx 容器提供，/api 请求由 nginx 反向代理到本服务。
 */
public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/time", exchange -> {
            String json = "{\"time\": \"" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\"}";
            send(exchange, 200, json, "application/json; charset=utf-8");
        });

        // 根路径返回接口说明，方便健康检查
        server.createContext("/", exchange ->
                send(exchange, 200, "Java 后端 API 已运行，接口: GET /api/time", "text/plain; charset=utf-8"));

        server.setExecutor(null);
        server.start();
        System.out.println("后端 API 已启动: http://localhost:" + PORT);
    }

    private static void send(HttpExchange exchange, int code, String body, String type) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
