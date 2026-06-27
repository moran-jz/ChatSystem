package server;


import java.net.ServerSocket;
import java.net.Socket;
import security.SecurityBootstrap;

public class ChatServer {

    private static final int PORT = 9000;

    public static void main(String[] args) {
        try {
            // 1. 初始化安全模块（B）
            SecurityBootstrap.init();

            // 2. 初始化扩展模块（D）
            ExtensionManager.init();

            // 3. 启动核心服务器（A）
            startServer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startServer() throws Exception {
        Log.initLog();

        // 使用 try-with-resources 管理 ServerSocket，避免资源泄漏警告
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat Server started on port " + PORT);
            
            while (true) {
                // 接收客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println(clientSocket.getRemoteSocketAddress()+" connect.");

                //写入日志
                Log.add(clientSocket.getRemoteSocketAddress()+" connect.");

                // 多线程处理每个客户端
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start(); 
                
            }
        }
    }
}