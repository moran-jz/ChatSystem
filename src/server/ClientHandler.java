package server;

import java.net.*;
import java.io.*;
import log.Logger;
import admin.*;
import security.SecurityBootstrap;   // 假设您的安全模块入口类

/**
 * 客户端处理器（每个客户端一个线程）
 * 完整实现登录认证、消息转发、管理员命令处理。
 */
public class ClientHandler extends Thread {
    private final Socket socket;
    private final ChatServer server;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String username;           // 登录后的用户名

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            Logger.getInstance().error("Failed to initialize streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // ---------- 登录流程（含安全验证） ----------
            // 协议：客户端先发送用户名（UTF），再发送密码（UTF）
            String username = dis.readUTF();
            String password = dis.readUTF();

            // 1. 检查封禁状态（即使未登录也可查，防止暴力破解）
            if (server.isBanned(username)) {
                sendMessage("您的账号已被封禁，无法登录。");
                closeConnection();
                return;
            }

            // 2. 调用安全模块验证密码
            boolean authSuccess = SecurityBootstrap.authenticate(username, password);
            if (!authSuccess) {
                sendMessage("用户名或密码错误。");
                closeConnection();
                return;
            }

            // 3. 验证通过，注册用户
            this.username = username;
            server.addClient(username, this);
            sendMessage("登录成功！欢迎 " + username);
            server.broadcastToAll(username + " 加入了聊天室。");

            // ---------- 主消息循环 ----------
            String message;
            while ((message = dis.readUTF()) != null) {
                // 处理管理员命令（以 '/' 开头）
                if (message.startsWith("/")) {
                    AdminCommandHandler adminHandler = ExtensionManager.getAdminCommandHandler();
                    if (adminHandler != null) {
                        String response = adminHandler.handleCommand(message);
                        sendMessage(response);
                    } else {
                        sendMessage("管理员命令处理器未初始化。");
                    }
                } else {
                    // 普通消息：广播给所有人（可扩展私聊逻辑）
                    server.broadcastToAll(username + ": " + message);
                }
            }
        } catch (EOFException e) {
            // 客户端正常断开
            Logger.getInstance().info((username != null ? username : "未知用户") + " 断开连接");
        } catch (IOException e) {
            Logger.getInstance().error("ClientHandler error: " + e.getMessage());
        } finally {
            // 清理资源
            if (username != null && !username.isEmpty()) {
                server.removeClient(username);
                server.broadcastToAll(username + " 离开了聊天室。");
            }
            closeConnection();
        }
    }

    /**
     * 发送消息给该客户端
     */
    public void sendMessage(String msg) {
        if (dos != null) {
            try {
                dos.writeUTF(msg);
                dos.flush();
            } catch (IOException e) {
                Logger.getInstance().error("Failed to send message to " + username + ": " + e.getMessage());
            }
        }
    }

    /**
     * 关闭连接，释放资源
     */
    public void closeConnection() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            Logger.getInstance().error("Error closing connection: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }
}