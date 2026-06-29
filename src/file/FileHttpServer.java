package file;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import log.Logger;
/**
 * 简易 HTTP 文件服务器，提供文件上传（POST /upload）和下载（GET /download/{filename}）。
 * 文件存储于 server_files/ 目录。
 */
public class FileHttpServer {
    private static final int PORT = 8080;
    private static final String STORAGE_DIR = "server_files/";
    private HttpServer server;

    public void start() throws IOException {
        // 创建存储目录
        File storage = new File(STORAGE_DIR);
        if (!storage.exists()) {
            storage.mkdirs();
        }

        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        Logger.getInstance().info("File HTTP Server started on port " + PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            Logger.getInstance().info("File HTTP Server stopped.");
        }
    }

    // 处理文件上传
    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            // 从 URL 参数获取文件名，例如 /upload?filename=myfile.txt
            String query = exchange.getRequestURI().getQuery();
            String filename = "uploaded_" + System.currentTimeMillis();
            if (query != null && query.startsWith("filename=")) {
                filename = URLDecoder.decode(query.substring(9), StandardCharsets.UTF_8.name());
            }
            filename = new File(filename).getName();

            // 保存文件
            Path filePath = Paths.get(STORAGE_DIR, filename);
            try (InputStream is = exchange.getRequestBody();
                 FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            String response = "File uploaded successfully: " + filename;
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            Logger.getInstance().info("File uploaded: " + filename);
        }
    }

    // 处理文件下载
    static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String filename = URLDecoder.decode(path.substring(path.lastIndexOf('/') + 1), StandardCharsets.UTF_8.name());
            filename = new File(filename).getName();
            if (filename.isEmpty()) {
                exchange.sendResponseHeaders(400, -1); // Bad Request
                return;
            }

            Path filePath = Paths.get(STORAGE_DIR, filename);
            File file = filePath.toFile();
            if (!file.exists()) {
                exchange.sendResponseHeaders(404, -1); // Not Found
                return;
            }

            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            Logger.getInstance().info("File downloaded: " + filename);
        }
    }

    // 独立运行测试
    public static void main(String[] args) throws IOException {
        new FileHttpServer().start();
    }
}
