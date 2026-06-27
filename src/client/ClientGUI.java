package client;

import javax.swing.*;

/**
 * 全局 UI 工具类，用于将服务器消息安全地更新到聊天窗口。
 */
public class ClientGUI {
    private static JTextArea chatArea;

    /**
     * 注册聊天文本区域（由 ChatWindow 在初始化时调用）。
     * @param area 聊天窗口中的 JTextArea 实例
     */
    public static void registerChatArea(JTextArea area) {
        chatArea = area;
    }

    /**
     * 接收服务器消息，并在 EDT（事件派发线程）中更新 UI。
     * @param msg 服务器发来的原始消息
     */
    public static void onMessage(String msg) {
        if (chatArea != null) {
            SwingUtilities.invokeLater(() -> {
                chatArea.append(msg + "\n");
                // 自动滚动到底部
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            });
        }
    }
}