package common;

/**
 * 通用消息对象，封装协议中的 TYPE|sender|receiver|content。
 */
public class Message {
    private final String type;
    private final String sender;
    private final String receiver;
    private final String content;

    public Message(String type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    public String getType()     { return type; }
    public String getSender()   { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent()  { return content; }

    /**
     * 将一行字符串解析为 Message 对象。
     * 格式：TYPE|sender|receiver|content
     */
    public static Message decode(String line) {
        if (line == null || line.isEmpty()) return null;
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4) return null;
        return new Message(parts[0], parts[1], parts[2], parts[3]);
    }

    /**
     * 将 Message 编码为传输字符串（带换行符，由调用方添加）。
     */
    public String encode() {
        return type + "|" + sender + "|" + receiver + "|" + content;
    }
}