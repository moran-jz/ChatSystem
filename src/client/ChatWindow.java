package client;

import common.Protocol;
import file.FileClient;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ChatWindow extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendBtn;
    private JButton logoutBtn;
    private JButton fileBtn;
    private ClientConnection connection;
    private String username;

    public ChatWindow(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;
        initUI();
        appendMessage("系统", "欢迎 " + username + "，您已登录");
    }

    public String getUsername() {
        return username;
    }

    private void initUI() {
        setTitle("聊天室 - " + username);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendBtn = new JButton("发送");
        logoutBtn = new JButton("退出");
        fileBtn = new JButton("📎");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(fileBtn);
        btnPanel.add(sendBtn);
        btnPanel.add(logoutBtn);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(btnPanel, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendMessage());
        logoutBtn.addActionListener(e -> logout());
        fileBtn.addActionListener(e -> sendFile());
        inputField.addActionListener(e -> sendMessage());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                logout();
            }
        });
    }

    private void sendMessage() {
        String content = inputField.getText().trim();
        if (content.isEmpty()) return;

        setSendEnabled(false);
        inputField.setEnabled(false);

        new Thread(() -> {
            try {
                String message = Protocol.BROADCAST + "|" + username + "|all|" + content;
                connection.send(message);

                SwingUtilities.invokeLater(() -> {
                    appendMessage(username, content);
                });

                SwingUtilities.invokeLater(() -> {
                    inputField.setText("");
                    setSendEnabled(true);
                    inputField.setEnabled(true);
                    inputField.requestFocus();
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(ChatWindow.this,
                            "发送失败: " + e.getMessage());
                    setSendEnabled(true);
                    inputField.setEnabled(true);
                });
            }
        }).start();
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null || file.length() > 50 * 1024 * 1024) {
            JOptionPane.showMessageDialog(this, "文件过大（最大50MB）");
            return;
        }

        // 默认广播给所有人，不再弹窗选择
        final String finalTarget = "all";
        final File finalFile = file;

        setSendEnabled(false);
        fileBtn.setEnabled(false);

        new Thread(() -> {
            try {
                FileClient fileClient = new FileClient();
                boolean uploaded = fileClient.uploadFile(
                        finalFile.getAbsolutePath(),
                        ChatClient.getFileServerBaseUrl(),
                        finalFile.getName()
                );
                if (!uploaded) {
                    throw new IllegalStateException("HTTP 文件上传失败");
                }

                String message = "FILE|" + username + "|" + (finalTarget.isEmpty() ? "all" : finalTarget) + "|"
                        + finalFile.getName();
                connection.send(message);

                SwingUtilities.invokeLater(() -> {
                    appendMessage("系统", "文件 " + finalFile.getName() + " 上传成功，已通知其他用户下载");
                    setSendEnabled(true);
                    fileBtn.setEnabled(true);
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(ChatWindow.this,
                            "文件上传失败: " + e.getMessage());
                    setSendEnabled(true);
                    fileBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private void logout() {
        new Thread(() -> {
            try {
                connection.send(Protocol.LOGOUT + "|" + username + "||");
            } catch (Exception ignored) {}
        }).start();
        ClientGUI.setCurrentWindow(null);
        dispose();
    }

    public void appendMessage(String sender, String content) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + sender + "] " + content + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void appendGroupMessage(String content) {
        appendMessage("群聊", content);
    }

    private void setSendEnabled(boolean enabled) {
        sendBtn.setEnabled(enabled);
        fileBtn.setEnabled(enabled);
    }
}
