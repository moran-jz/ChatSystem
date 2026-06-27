package security;

/**
 * 认证结果枚举。
 * <p>
 * 用于 UserManager 和 AuthService 向调用方（如 ClientHandler）
 * 返回注册 / 登录操作的明确结果。调用方根据 {@code code} 决定
 * 给客户端返回 SUCCESS 还是 FAIL，并可通过 {@code message}
 * 携带可读的原因描述。
 * </p>
 *
 * <pre>
 * 使用示例：
 *   AuthResult r = UserManager.getInstance().login("alice", "123");
 *   if (r == AuthResult.SUCCESS) { ... }
 *   else { System.out.println(r.getMessage()); }
 * </pre>
 */
public enum AuthResult {

    /** 操作成功 */
    SUCCESS(true, "操作成功"),

    /** 用户名已存在（注册时） */
    USER_ALREADY_EXISTS(false, "用户名已存在"),

    /** 用户名或密码为空 / 格式不合法 */
    INVALID_CREDENTIALS(false, "用户名或密码不能为空"),

    /** 用户不存在（登录时） */
    USER_NOT_FOUND(false, "用户不存在"),

    /** 密码错误（登录时） */
    WRONG_PASSWORD(false, "密码错误"),

    /** 用户已在线（重复登录） */
    USER_ALREADY_ONLINE(false, "用户已在线"),

    /** 用户未登录（操作需要登录态时） */
    USER_NOT_ONLINE(false, "用户未登录"),

    /** 未知错误 */
    UNKNOWN_ERROR(false, "未知错误");

    private final boolean success;
    private final String message;

    AuthResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * 操作是否成功。
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取可读的原因描述。
     *
     * @return 中文描述信息
     */
    public String getMessage() {
        return message;
    }
}
