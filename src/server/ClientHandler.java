package server;

import java.net.*;
import java.io.*;
import log.Logger;
import admin.*;
import security.*;

/**
 * 客户端处理器（每个客户端一个线程）
 * 完整实现登录认证、注册、消息转发、管理员命令处理。
 * 注册成功后不会自动登录，需用户重新登录。
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
            // 1. 读取操作类型
            String operation = dis.readUTF();  // "LOGIN" 或 "REGISTER"
            String username = dis.readUTF();
            String password = dis.readUTF();

            // 检查封禁（登录和注册都检查）
            if (server.isBanned(username)) {
                sendMessage("您的账号已被封禁，无法操作。");
                closeConnection();
                return;
            }

            // ---------- 处理注册（不自动登录） ----------
            if ("REGISTER".equals(operation)) {
                AuthResult result = UserManager.getInstance().register(username, password);
                if (result.isSuccess()) {
                    sendMessage("注册成功，请重新登录。");
                } else {
                    sendMessage("注册失败：" + result.getMessage());
                }
                // 关闭输出流，表示不会再发送数据
                socket.shutdownOutput();
                // 等待客户端主动关闭连接
                try {
                    while (dis.read() != -1) {
                        // 忽略客户端可能发送的数据，直到客户端关闭
                    }
                } catch (IOException e) {
                    // 客户端断开，正常
                }
                // 注册完成后，线程结束，finally 会关闭资源
                return;
            }

            // ---------- 处理登录 ----------
            if ("LOGIN".equals(operation)) {
                boolean success = SecurityBootstrap.authenticate(username, password);
                if (success) {
                    sendMessage("登录成功！");
                } else {
                    sendMessage("用户名或密码错误。");
                    closeConnection();
                    return;
                }
            } else {
                sendMessage("未知操作类型。");
                closeConnection();
                return;
            }

            // 登录成功后注册到在线列表
            this.username = username;
            server.addClient(username, this);
            sendMessage("欢迎 " + username);
            server.broadcastToAll(username + " 加入了聊天室。");

            // ---------- 主消息循环 ----------
            String message;
            while ((message = dis.readUTF()) != null) {
                // 处理管理员命令（传入当前用户名，用于权限校验）
                if (message.startsWith("/")) {
                    AdminCommandHandler adminHandler = ExtensionManager.getAdminCommandHandler();
                    if (adminHandler != null) {
                        String response = adminHandler.handleCommand(message, username);
                        sendMessage(response);
                    } else {
                        sendMessage("管理员命令处理器未初始化。");
                    }
                } else {
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