package admin;
import java.util.List;
import server.ChatServer;

/**
 * 管理员命令处理器，解析以 '/' 开头的命令并执行相应操作。
 * 需要 ChatServer 提供以下方法：
 *   - broadcastToAll(String message)
 *   - kickUser(String username)
 *   - banUser(String username)
 *   - unbanUser(String username)
 *   - getOnlineUsers() 返回 List<String>
 *   - shutdown() 关闭服务器（可选）
 */
public class AdminCommandHandler {

    private final ChatServer chatServer;

    public AdminCommandHandler(ChatServer chatServer) {
        this.chatServer = chatServer;
    }

    /**
     * 处理一条命令消息（原始字符串）
     * @param fullMessage 例如 "/kick Tom" 或 "/broadcast Hello everyone"
     * @return 执行结果的描述（可用于回复给命令发起者）
     */
    public String handleCommand(String fullMessage) {
        if (fullMessage == null || !fullMessage.startsWith("/")) {
            return "Not a command.";
        }

        String[] parts = fullMessage.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "/kick":
                return kickUser(arg.trim());
            case "/ban":
                return banUser(arg.trim());
            case "/unban":
                return unbanUser(arg.trim());
            case "/list":
                return listUsers();
            case "/broadcast":
                return broadcastMessage(arg);
            case "/shutdown":
                return shutdownServer();
            default:
                return "Unknown command: " + command;
        }
    }

    private String kickUser(String username) {
        if (username.isEmpty()) {
            return "Usage: /kick <username>";
        }
        boolean result = chatServer.kickUser(username);
        if (result) {
            chatServer.broadcastToAll("User " + username + " has been kicked by admin.");
            return "User " + username + " kicked.";
        } else {
            return "Failed to kick " + username + ". User may not exist.";
        }
    }

    private String banUser(String username) {
        if (username.isEmpty()) {
            return "Usage: /ban <username>";
        }
        boolean result = chatServer.banUser(username);
        if (result) {
            chatServer.broadcastToAll("User " + username + " has been banned by admin.");
            return "User " + username + " banned.";
        } else {
            return "Failed to ban " + username + ".";
        }
    }

    private String unbanUser(String username) {
        if (username.isEmpty()) {
            return "Usage: /unban <username>";
        }
        boolean result = chatServer.unbanUser(username);
        if (result) {
            return "User " + username + " unbanned.";
        } else {
            return "Failed to unban " + username + ".";
        }
    }

    private String listUsers() {
        List<String> users = chatServer.getOnlineUsers();
        if (users.isEmpty()) {
            return "No online users.";
        }
        return "Online users: " + String.join(", ", users);
    }

    private String broadcastMessage(String message) {
        if (message.isEmpty()) {
            return "Usage: /broadcast <message>";
        }
        chatServer.broadcastToAll("[Admin] " + message);
        return "Broadcast sent.";
    }

    private String shutdownServer() {
        // 可选，需 ChatServer 提供 shutdown() 方法
        // chatServer.shutdown();
        return "Server shutdown not implemented in this handler.";
    }
}