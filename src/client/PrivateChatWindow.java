package client;

import common.Message;
import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PrivateChatWindow extends JFrame {
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final ClientConnection connection;
    private final String currentUser;
    private final String targetUser;

    public PrivateChatWindow(ClientConnection connection, String currentUser, String targetUser) {
        this.connection = connection;
        this.currentUser = currentUser;
        this.targetUser = targetUser;

        ClientGUI.registerPrivateChat(targetUser, this);

        setTitle("私聊 - " + targetUser);
        setSize(480, 360);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);
        setLayout(new BorderLayout(8, 8));

        JLabel header = new JLabel("正在与 " + targetUser + " 私聊");
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        add(header, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        inputField.addActionListener(e -> sendPrivateMessage());

        JButton sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendPrivateMessage());

        bottom.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                ClientGUI.unregisterPrivateChat(targetUser);
            }
        });
    }

    private void sendPrivateMessage() {
        String content = inputField.getText().trim();
        if (content.isEmpty()) return;

        try {
            Message msg = new Message(Protocol.PRIVATE, currentUser, targetUser, content);
            connection.send(msg.encode());
            appendOwnMessage(content);
            inputField.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "私聊发送失败: " + e.getMessage());
        }
    }

    public void appendIncomingMessage(String sender, String content) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + sender + "] " + content + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void appendOwnMessage(String content) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[我] " + content + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
}