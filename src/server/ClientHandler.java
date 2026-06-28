package server;

import admin.AdminCommandHandler;
import common.Message;
import common.Protocol;
import log.Logger;
import security.AuthResult;
import security.SecurityBootstrap;
import security.UserManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

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
    private String username;

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
            String operation = dis.readUTF();
            String username = dis.readUTF();
            String password = dis.readUTF();

            if (server.isBanned(username)) {
                sendMessage("您的账号已被封禁，无法操作。");
                closeConnection();
                return;
            }

            if ("REGISTER".equals(operation)) {
                AuthResult result = UserManager.getInstance().register(username, password);
                if (result.isSuccess()) {
                    sendMessage("注册成功，请重新登录。");
                } else {
                    sendMessage("注册失败：" + result.getMessage());
                }

                socket.shutdownOutput();
                try {
                    while (dis.read() != -1) {
                    }
                } catch (IOException ignored) {
                }
                return;
            }

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

            this.username = username;
            server.addClient(username, this);
            sendMessage("欢迎 " + username);
            server.broadcastToAll(username + " 加入了聊天室。");
            server.pushOnlineUsers();

            String message;
            while ((message = dis.readUTF()) != null) {
                Message protocolMessage = Message.decode(message);
                if (protocolMessage != null && Protocol.PRIVATE.equals(protocolMessage.getType())) {
                    server.sendPrivateMessage(this.username, protocolMessage.getReceiver(), protocolMessage.getContent());
                    continue;
                }

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
            Logger.getInstance().info((username != null ? username : "未知用户") + " 断开连接");
        } catch (IOException e) {
            Logger.getInstance().error("ClientHandler error: " + e.getMessage());
        } finally {
            if (username != null && !username.isEmpty()) {
                server.removeClient(username);
                server.broadcastToAll(username + " 离开了聊天室。");
                server.pushOnlineUsers();
            }
            closeConnection();
        }
    }

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

    public void closeConnection() {
        try {
            if (dis != null) {
                dis.close();
            }
            if (dos != null) {
                dos.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.getInstance().error("Error closing connection: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }
}