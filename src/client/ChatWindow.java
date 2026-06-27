package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ChatWindow extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private ClientConnection connection;
    private String username;

    public ChatWindow(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;
        setTitle("聊天室 - " + username);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(chatArea);
        add(scroll, BorderLayout.CENTER);

        // 注册聊天区域（供 ClientGUI 使用）
        ClientGUI.registerChatArea(chatArea);

        JPanel bottom = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendBtn = new JButton("发送");
        sendBtn.addActionListener(e -> sendMessage());
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ChatClient.shutdown();
            }
        });

        chatArea.append("[系统] 欢迎 " + username + "，开始聊天吧！\n");
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        try {
            connection.send(msg);
            inputField.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "发送失败: " + e.getMessage());
        }
    }
}