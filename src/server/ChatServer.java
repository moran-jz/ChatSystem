package server;

import java.util.*;
import java.net.*;
import java.io.*;
import log.Logger;
import java.util.concurrent.*;
import security.SecurityBootstrap;

public class ChatServer {
    private static ChatServer instance;
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
    // 线程安全的在线用户映射
    private final Map<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();
    private final Set<String> bannedUsers = Collections.synchronizedSet(new HashSet<>());
    private final List<String> messageHistory = new CopyOnWriteArrayList<>();

    private ChatServer(int port) {
        this.port = port;
        this.running = false;
    }

    public static ChatServer getInstance() {
        return instance;
    }

    public static void createInstance(int port) {
        if (instance == null) {
            instance = new ChatServer(port);
        }
    }
    public static void main(String[] args) {
        try {
            // 1. 初始化安全模块（B）
            SecurityBootstrap.init();

            // 2. 创建 ChatServer 实例（单例）
            ChatServer.createInstance(9000);
            // 无需再调用 setChatServer，因为 ExtensionManager.init() 内部会通过 getInstance() 获取

            // 3. 初始化扩展模块（D）
            ExtensionManager.init();

            // 4. 启动服务器核心
            ChatServer.getInstance().start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 实例方法：启动服务器循环
     */
    public void start() throws IOException {
        this.running = true;
        // 使用 try-with-resources 管理 ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket;   // 保留引用供 shutdown 使用
            System.out.println("Chat Server started on port " + port);
            Log.initLog();   // 假设您的 Log 类存在

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(clientSocket.getRemoteSocketAddress() + " connect.");
                Log.add(clientSocket.getRemoteSocketAddress() + " connect.");

                // 传入 this 引用，让 ClientHandler 能调用广播等方法
                ClientHandler handler = new ClientHandler(clientSocket, this);
                handler.start();
            }
        } catch (IOException e) {
            if (running) {
                throw e;
            }
        } finally {
            // 确保关闭所有客户端连接
            shutdown();
        }
    }

    // ---------- 客户端管理辅助方法 ----------
    public void addClient(String username, ClientHandler handler) {
        onlineClients.put(username, handler);
        Logger.getInstance().info("User " + username + " logged in.");
    }

    public void removeClient(String username) {
        onlineClients.remove(username);
        Logger.getInstance().info("User " + username + " disconnected.");
    }

    public boolean isBanned(String username) {
        return bannedUsers.contains(username);
    }

    // ---------- 管理员命令相关方法 ----------
    /**
     * 向所有在线客户端广播消息（包括系统消息）
     */
    public void broadcastToAll(String message) {
        if (message == null || message.isEmpty()) return;
        String formatted = "[系统] " + message;
        for (ClientHandler handler : onlineClients.values()) {
            try {
                handler.sendMessage(formatted);
            } catch (Exception e) {
                Logger.getInstance().error("广播失败给 " + handler.getUsername() + ": " + e.getMessage());
            }
        }
        messageHistory.add(formatted);
        Logger.getInstance().info("广播: " + message);
    }

    /**
     * 踢出用户（从在线列表移除，关闭连接）
     */
    public boolean kickUser(String username) {
        if (username == null || username.isEmpty()) return false;
        ClientHandler handler = onlineClients.remove(username);
        if (handler != null) {
            try {
                handler.sendMessage("您已被管理员踢出服务器。");
                handler.closeConnection();
            } catch (Exception e) {
                Logger.getInstance().error("踢出用户出错: " + e.getMessage());
            }
            Logger.getInstance().info("用户 " + username + " 被管理员踢出。");
            broadcastToAll(username + " 被管理员踢出。");
            return true;
        }
        return false;
    }

    /**
     * 封禁用户（加入黑名单并踢出）
     */
    public boolean banUser(String username) {
        if (username == null || username.isEmpty()) return false;
        bannedUsers.add(username);
        ClientHandler handler = onlineClients.remove(username);
        if (handler != null) {
            try {
                handler.sendMessage("您已被管理员封禁。");
                handler.closeConnection();
            } catch (Exception e) {
                Logger.getInstance().error("封禁用户出错: " + e.getMessage());
            }
        }
        Logger.getInstance().info("用户 " + username + " 被封禁。");
        broadcastToAll(username + " 已被管理员封禁。");
        return true;
    }

    /**
     * 解封用户（从黑名单移除）
     */
    public boolean unbanUser(String username) {
        if (username == null || username.isEmpty()) return false;
        boolean removed = bannedUsers.remove(username);
        if (removed) {
            Logger.getInstance().info("用户 " + username + " 已解封。");
        }
        return removed;
    }

    /**
     * 获取当前在线用户列表
     */
    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineClients.keySet());
    }

    /**
     * 关闭服务器（优雅停机）
     */
    public void shutdown() {
        if (!running) return;
        running = false;
        Logger.getInstance().info("服务器正在关闭...");

        // 向所有客户端发送停机通知并断开
        for (ClientHandler handler : onlineClients.values()) {
            try {
                handler.sendMessage("服务器正在关闭，请重新连接。");
                handler.closeConnection();
            } catch (Exception e) { /* 忽略 */ }
        }
        onlineClients.clear();

        // 关闭 ServerSocket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Logger.getInstance().error("关闭 ServerSocket 出错: " + e.getMessage());
        }
        Logger.getInstance().info("服务器已关闭。");
    }
}