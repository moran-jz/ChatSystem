package common;

/**
 * 协议常量定义，与 README 中的通信格式保持一致。
 */
public final class Protocol {
    private Protocol() {}

    // 消息类型
    public static final String LOGIN    = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String LOGOUT   = "LOGOUT";
    public static final String PRIVATE  = "PRIVATE";
    public static final String BROADCAST = "BROADCAST";
    public static final String SUCCESS  = "SUCCESS";
    public static final String FAIL     = "FAIL";
}