package client;

import file.FileClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

public class ChatWindow extends JFrame {
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final DefaultListModel<String> userListModel;
    private final JList<String> userList;
    private final ClientConnection connection;
    private final String username;

    public ChatWindow(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;

        setTitle("聊天室 - " + username);
        setSize(760, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane chatScroll = new JScrollPane(chatArea);

        JPanel chatPanel = new JPanel(new BorderLayout(0, 8));
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());

        JButton sendBtn = new JButton("发送");
        sendBtn.addActionListener(e -> sendMessage());

        JButton fileBtn = new JButton("文件");
        fileBtn.addActionListener(e -> sendFile());

        JPanel rightPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        rightPanel.add(fileBtn);
        rightPanel.add(sendBtn);

        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(rightPanel, BorderLayout.EAST);
        chatPanel.add(bottom, BorderLayout.SOUTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openPrivateChat();
                }
            }
        });

        JButton privateChatBtn = new JButton("发起私聊");
        privateChatBtn.addActionListener(e -> openPrivateChat());

        JPanel usersPanel = new JPanel(new BorderLayout(0, 8));
        usersPanel.setBorder(BorderFactory.createTitledBorder("在线用户"));
        usersPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        usersPanel.add(privateChatBtn, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatPanel, usersPanel);
        splitPane.setResizeWeight(0.78);
        splitPane.setDividerLocation(560);
        add(splitPane, BorderLayout.CENTER);

        ClientGUI.registerMainWindow(this, connection, username);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ClientGUI.unregisterMainWindow(ChatWindow.this);
                ChatClient.shutdown();
            }
        });

        appendGroupMessage("[系统] 欢迎 " + username + "，开始聊天吧！");
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) {
            return;
        }

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

    private void openPrivateChat() {
        String targetUser = userList.getSelectedValue();
        if (targetUser == null || targetUser.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先从右侧选择一个在线用户");
            return;
        }
        ClientGUI.openPrivateChat(targetUser);
    }

    public void appendGroupMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void updateOnlineUsers(List<String> users) {
        String selected = userList.getSelectedValue();
        userListModel.clear();

        for (String user : users) {
            if (!username.equals(user)) {
                userListModel.addElement(user);
            }
        }

        if (selected != null && userListModel.indexOf(selected) >= 0) {
            userList.setSelectedValue(selected, true);
        }
    }
}