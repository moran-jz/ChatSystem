package admin;

import server.ChatServer;
import java.util.Set;
import java.util.HashSet;

/**
 * 管理员命令处理器（带权限校验）
 */
public class AdminCommandHandler {
    private final ChatServer chatServer;
    private final Set<String> admins = new HashSet<>();

    public AdminCommandHandler(ChatServer chatServer) {
        this.chatServer = chatServer;
        // 硬编码管理员账号（可根据需要改为从文件加载）
        admins.add("admin");
        // 可添加更多管理员
        admins.add("root");
    }

    /**
     * 处理命令，并校验权限
     * @param fullMessage 原始消息（如 "/kick tom"）
     * @param sender      命令发起者的用户名
     * @return 执行结果字符串（直接回复给发起者）
     */
    public String handleCommand(String fullMessage, String sender) {
        if (fullMessage == null || !fullMessage.startsWith("/")) {
            return "不是有效命令。";
        }

        String[] parts = fullMessage.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";

        // /help 命令不受权限限制
        if ("/help".equals(command)) {
            return getHelp();
        }

        // 权限校验
        if (!admins.contains(sender)) {
            return "权限不足，只有管理员可以执行此命令。";
        }

        switch (command) {
            case "/kick":
                return kick(arg.trim());
            case "/ban":
                return ban(arg.trim());
            case "/unban":
                return unban(arg.trim());
            case "/list":
                return listUsers();
            case "/broadcast":
                return broadcastMessage(arg);
            case "/shutdown":
                return shutdownServer();
            default:
                return "未知命令，输入 /help 查看帮助。";
        }
    }

    // -------------------- 具体命令实现 --------------------//

    private String kick(String username) {
        if (username.isEmpty()) return "用法: /kick <用户名>";
        boolean result = chatServer.kickUser(username);
        if (result) {
            chatServer.broadcast("用户 " + username + " 已被管理员踢出。");
            return "用户 " + username + " 已踢出。";
        }
        return "踢出失败，用户可能不在线或不存在。";
    }

    private String ban(String username) {
        if (username.isEmpty()) return "用法: /ban <用户名>";
        boolean result = chatServer.banUser(username);
        if (result) {
            chatServer.broadcast("用户 " + username + " 已被管理员封禁。");
            return "用户 " + username + " 已封禁。";
        }
        return "封禁失败（用户可能已封禁或不存在）。";
    }

    private String unban(String username) 
    {
        if (username.isEmpty()) return "用法: /unban <用户名>";
        boolean result = chatServer.unbanUser(username);
        return result ? "用户 " + username + " 已解封。" : "解封失败，用户不在封禁列表中。";
    }

    private String listUsers() {
        Set<String> users = chatServer.getOnlineUsers();
        if (users.isEmpty()) return "当前没有在线用户。";
        return "在线用户: " + String.join(", ", users);
    }

    private String broadcastMessage(String msg) {
        if (msg.isEmpty()) return "用法: /broadcast <消息内容>";
        chatServer.broadcast("[管理员广播] " + msg);
        return "广播已发送。";
    }

    private String shutdownServer() {
        chatServer.shutdown();
        return "服务器正在关闭...";
    }

    private String getHelp() {
        return "可用命令:\n" +
                "/kick <用户名>  - 踢出用户\n" +
                "/ban <用户名>   - 封禁用户\n" +
                "/unban <用户名> - 解封用户\n" +
                "/list          - 查看在线用户\n" +
                "/broadcast <消息> - 发送系统广播\n" +
                "/shutdown      - 关闭服务器\n" +
                "/help          - 显示本帮助";
    }
}