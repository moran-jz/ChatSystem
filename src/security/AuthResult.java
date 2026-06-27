package security;

/**
 * 认证操作的结果封装。
 */
public class AuthResult {
    private final boolean success;
    private final String message;

    private AuthResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static AuthResult success() {
        return new AuthResult(true, null);
    }

    public static AuthResult fail(String reason) {
        return new AuthResult(false, reason);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}