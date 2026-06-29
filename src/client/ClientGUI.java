package client;

import common.Message;
import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;

public class ClientGUI {
    private static ChatWindow currentWindow;
    private static final ConcurrentHashMap<String, PrivateChatWindow> privateChats = new ConcurrentHashMap<>();

    public static void setCurrentWindow(ChatWindow window) {
        currentWindow = window;
    }

    public static void registerPrivateChat(String targetUser, PrivateChatWindow window) {
        privateChats.put(targetUser, window);
    }

    public static void unregisterPrivateChat(String targetUser) {
        privateChats.remove(targetUser);
    }

    public static PrivateChatWindow getPrivateChat(String targetUser) {
        return privateChats.get(targetUser);
    }

    public static void onMessage(String raw) {
        if (raw == null || raw.isEmpty()) return;

        System.out.println("ClientGUI received: " + raw);

        Message msg = Message.decode(raw);
        if (msg != null) {
            String type = msg.getType();
            String sender = msg.getSender();
            String receiver = msg.getReceiver();
            String content = msg.getContent();

            // ---- 私聊 ----
            if (Protocol.PRIVATE.equals(type)) {
                String currentUser = (currentWindow != null) ? currentWindow.getUsername() : null;
                String target = null;
                if (currentUser != null && currentUser.equals(sender)) {
                    target = receiver;
                } else if (currentUser != null && currentUser.equals(receiver)) {
                    target = sender;
                }
                if (target != null) {
                    PrivateChatWindow pWindow = privateChats.get(target);
                    if (pWindow != null) {
                        pWindow.appendIncomingMessage(sender, content);
                    }
                }
                return;
            }

            // ---- 广播/群聊 ----
            if (Protocol.BROADCAST.equals(type) || "GROUP".equals(type)) {
                if (currentWindow != null) {
                    currentWindow.appendMessage(sender, content);
                }
                return;
            }

            // ---- 文件请求（收到别人发来的文件请求） ----
            if ("FILE_REQUEST".equals(type)) {
                try {
                    String decodedFilename = URLDecoder.decode(content, "UTF-8");
                    String senderName = sender;
                    int response = JOptionPane.showConfirmDialog(
                            currentWindow,
                            "用户 " + senderName + " 想发送文件: " + decodedFilename + "\n是否下载？",
                            "文件请求",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (response == JOptionPane.YES_OPTION) {
                        String encoded = URLEncoder.encode(decodedFilename, "UTF-8");
                        String url = "http://127.0.0.1:8080/files/" + encoded;
                        Desktop.getDesktop().browse(java.net.URI.create(url));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(currentWindow, "文件请求处理失败: " + ex.getMessage());
                }
                return;
            }

            // ---- 系统消息 ----
            if ("SUCCESS".equals(type) || "FAIL".equals(type) || "ERROR".equals(type)) {
                if (currentWindow != null) {
                    currentWindow.appendMessage("系统", content);
                }
                return;
            }
        }

        if (currentWindow != null) {
            currentWindow.appendMessage("系统", raw);
        }
    }
}