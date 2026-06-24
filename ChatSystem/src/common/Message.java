package common;

public class Message {

    private String type;       // LOGIN / PRIVATE / GROUP / FILE
    private String sender;
    private String receiver;
    private String content;

    public Message() {}

    public Message(String type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    // ================= getters & setters =================

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // ================= encode =================
    // Java对象 → 网络字符串
    public String encode() {
        return type + "|" + sender + "|" + receiver + "|" + content;
    }

    // ================= decode =================
    // 网络字符串 → Java对象
    public static Message decode(String msg) {
        if (msg == null || msg.isEmpty()) {
            return null;
        }

        String[] parts = msg.split("\\|", -1);

        // 防止越界
        String type = parts.length > 0 ? parts[0] : "";
        String sender = parts.length > 1 ? parts[1] : "";
        String receiver = parts.length > 2 ? parts[2] : "";
        String content = parts.length > 3 ? parts[3] : "";

        return new Message(type, sender, receiver, content);
    }

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}