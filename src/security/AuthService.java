package security;

import common.Message;
import common.Protocol;

/**
 * 认证服务 — ClientHandler 与安全模块之间的集成门面。
 *
 * <h3>设计意图</h3>
 * <p>
 * ClientHandler 只需调用 {@link #processAuth(Message)} 即可处理 LOGIN 和 REGISTER
 * 协议消息，无需直接了解 {@link UserManager} 或 {@link PasswordUtil} 的内部细节。
 * 返回值直接是协议格式字符串（{@code SUCCESS} / {@code FAIL:原因}），方便
 * ClientHandler 直接写回客户端。
 * </p>
 *
 * <h3>调用示例（在 ClientHandler 中）</h3>
 * <pre>
 *   Message msg = Message.decode(line);
 *   if (Protocol.LOGIN.equals(msg.getType()) || Protocol.REGISTER.equals(msg.getType())) {
 *       String response = AuthService.processAuth(msg);
 *       dos.writeUTF(response);
 *       dos.flush();
 *   }
 * </pre>
 */
public final class AuthService {

    private AuthService() {
        // 工具类，禁止实例化
    }

    /**
     * 处理认证相关消息（LOGIN / REGISTER）。
     *
     * @param msg 客户端发来的消息对象，sender 为用户名，content 为密码
     * @return 协议格式的响应字符串：
     *         <ul>
     *           <li>{@code SUCCESS} — 操作成功</li>
     *           <li>{@code FAIL:原因描述} — 操作失败</li>
     *         </ul>
     */
    public static String processAuth(Message msg) {
        if (msg == null) {
            return Protocol.FAIL + ":消息为空";
        }

        String type = msg.getType();
        String username = msg.getSender();
        String password = msg.getContent();

        UserManager um = UserManager.getInstance();

        switch (type) {
            case Protocol.REGISTER:
                return handleRegister(um, username, password);

            case Protocol.LOGIN:
                return handleLogin(um, username, password);

            case Protocol.LOGOUT:
                return handleLogout(um, username);

            default:
                return Protocol.FAIL + ":未知的认证类型";
        }
    }

    // ==================== 内部处理 ====================

    /**
     * 处理注册请求。
     */
    private static String handleRegister(UserManager um, String username, String password) {
        AuthResult result = um.register(username, password);
        if (result.isSuccess()) {
            return Protocol.SUCCESS;
        }
        return Protocol.FAIL + ":" + result.getMessage();
    }

    /**
     * 处理登录请求。
     * <p>
     * 注意：登录成功后，调用方（ClientHandler）还需将用户注册到
     * {@code OnlineUserManager} 中以完成会话建立。
     * </p>
     */
    private static String handleLogin(UserManager um, String username, String password) {
        AuthResult result = um.login(username, password);
        if (result.isSuccess()) {
            return Protocol.SUCCESS;
        }
        return Protocol.FAIL + ":" + result.getMessage();
    }

    /**
     * 处理退出请求。
     */
    private static String handleLogout(UserManager um, String username) {
        um.setUserOffline(username);
        return Protocol.SUCCESS;
    }
}
