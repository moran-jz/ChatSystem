package server;

import common.Message;
import common.Protocol;
import log.Logger;
import security.SecurityBootstrap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private static ChatServer instance;
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
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
            SecurityBootstrap.init();
            ChatServer.createInstance(9000);
            ExtensionManager.init();
            ChatServer.getInstance().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        this.running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket;
            Logger.getInstance().info("Chat Server started on port " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                Logger.getInstance().info(clientSocket.getRemoteSocketAddress() + " connect.");

                ClientHandler handler = new ClientHandler(clientSocket, this);
                handler.start();
            }
        } catch (IOException e) {
            if (running) {
                throw e;
            }
        } finally {
            shutdown();
        }
    }

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

    public void broadcastToAll(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

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

    public void sendPrivateMessage(String sender, String target, String content) {
        if (sender == null || target == null || content == null || target.isEmpty() || content.isEmpty()) {
            return;
        }

        ClientHandler targetHandler = onlineClients.get(target);
        if (targetHandler == null) {
            ClientHandler senderHandler = onlineClients.get(sender);
            if (senderHandler != null) {
                senderHandler.sendMessage("[系统] 用户 " + target + " 当前不在线，私聊发送失败。");
            }
            return;
        }

        String payload = new Message(Protocol.PRIVATE, sender, target, content).encode();
        targetHandler.sendMessage(payload);
        Logger.getInstance().info("私聊: " + sender + " -> " + target + ": " + content);
    }

    public void pushOnlineUsers() {
        List<String> users = getOnlineUsers();
        Collections.sort(users);
        String payload = "ONLINE_USERS|" + String.join(",", users);

        for (ClientHandler handler : onlineClients.values()) {
            handler.sendMessage(payload);
        }
    }

    public boolean kickUser(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

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

    public boolean banUser(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

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

    public boolean unbanUser(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        boolean removed = bannedUsers.remove(username);
        if (removed) {
            Logger.getInstance().info("用户 " + username + " 已解封。");
        }
        return removed;
    }

    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineClients.keySet());
    }

    public void shutdown() {
        if (!running) {
            return;
        }

        running = false;
        Logger.getInstance().info("服务器正在关闭...");

        for (ClientHandler handler : onlineClients.values()) {
            try {
                handler.sendMessage("服务器正在关闭，请重新连接。");
                handler.closeConnection();
            } catch (Exception ignored) {
            }
        }
        onlineClients.clear();

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