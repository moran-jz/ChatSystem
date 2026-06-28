package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import file.FileClient;

public class ChatWindow extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private ClientConnection connection;
    private String username;

    public ChatWindow(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;
        setTitle("聊天室 - " + username);
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // 聊天区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(chatArea);
        add(scroll, BorderLayout.CENTER);

        // 注册到 ClientGUI 静态引用
        ClientGUI.registerChatArea(chatArea);

        // 底部面板：输入框 + 发送按钮 + 文件按钮
        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        JButton sendBtn = new JButton("发送");
        sendBtn.addActionListener(e -> sendMessage());

        JButton fileBtn = new JButton("📎 文件");
        fileBtn.addActionListener(e -> sendFile());

        JPanel rightPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        rightPanel.add(fileBtn);
        rightPanel.add(sendBtn);

        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(rightPanel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ChatClient.shutdown();
            }
        });

        // 欢迎消息
        chatArea.append("[系统] 欢迎 " + username + "，开始聊天吧！\n");
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        // 处理本地下载命令
        if (msg.startsWith("/download ")) {
            String filename = msg.substring(10).trim();
            if (!filename.isEmpty()) {
                downloadFile(filename);
            } else {
                ClientGUI.onMessage("[系统] 请指定要下载的文件名");
            }
            inputField.setText("");
            return;
        }

        // 普通消息发送到服务器
        try {
            connection.send(msg);
            inputField.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "发送失败: " + e.getMessage());
        }
    }

    private void downloadFile(String filename) {
        String saveDir = "./downloads/";
        ClientGUI.onMessage("[系统] 正在下载文件: " + filename);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    FileClient fc = new FileClient();
                    boolean ok = fc.downloadFile("http://localhost:8080", filename, saveDir);
                    if (ok) {
                        ClientGUI.onMessage("[系统] 文件下载成功: " + filename + " (保存至 " + saveDir + ")");
                    } else {
                        ClientGUI.onMessage("[系统] 文件下载失败: " + filename + " (文件不存在或服务器错误)");
                    }
                } catch (Exception e) {
                    ClientGUI.onMessage("[系统] 下载异常: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            ClientGUI.onMessage("[系统] 正在上传文件: " + file.getName());

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    try {
                        FileClient fc = new FileClient();
                        boolean ok = fc.uploadFile(file.getAbsolutePath(), "http://localhost:8080", file.getName());
                        String status = ok ? "成功" : "失败";
                        ClientGUI.onMessage("[系统] 文件上传" + status + ": " + file.getName());
                        if (ok) {
                            // 广播文件上传通知（让其他用户知道）
                            connection.send("[文件] " + username + " 上传了文件: " + file.getName());
                        }
                    } catch (Exception ex) {
                        ClientGUI.onMessage("[系统] 文件上传异常: " + ex.getMessage());
                    }
                    return null;
                }
            }.execute();
        }
    }
}