package server;
import admin.*;
import java.net.*;
import java.io.*;
import log.Logger;

/**
 * 客户端处理器（每个客户端一个线程）
 * 负责：
 * - 接收客户端消息
 * - 处理登录认证
 * - 转发普通消息（群聊/私聊）
 * - 执行管理员命令（以 '/' 开头）
 */
public class ClientHandler extends Thread {
    private final Socket socket;
    private final ChatServer server;   // 核心：持有服务器引用
    private DataInputStream dis;
    private DataOutputStream dos;
    private String username;           // 登录后的用户名

    /**
     * 构造器
     * @param socket 客户端套接字
     * @param server ChatServer 实例（用于调用广播、踢人等）
     */
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
            // ---------- 登录流程 ----------
            // 约定：客户端先发送用户名，再发送密码（均以 UTF 字符串传输）
            String username = dis.readUTF();
            String password = dis.readUTF();

            // 检查封禁
            if (server.isBanned(username)) {
                sendMessage("您的账号已被封禁，无法登录。");
                closeConnection();
                return;
            }

            // 这里可以调用安全模块验证密码（当前示例简化，直接通过）
            // 实际应使用 SecurityBootstrap 验证
            // 假设验证通过：
            this.username = username;
            // 注册到服务器在线列表
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
            Logger.getInstance().info(username + " 断开连接");
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
     * @param msg 消息内容
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