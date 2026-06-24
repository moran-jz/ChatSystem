package client;

import javax.swing.*;

public class ClientGUI {

    private static JTextArea chatArea;

    public static void registerChatArea(JTextArea area) {
        chatArea = area;
    }

    public static void onMessage(String msg) {
        if (chatArea != null) {
            SwingUtilities.invokeLater(() -> {
                chatArea.append(msg + "\n");
            });
        }
    }
}