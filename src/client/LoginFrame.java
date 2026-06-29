package client;

import common.Message;
import common.Protocol;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginBtn;
    private JButton registerBtn;
    private JLabel statusLabel;
    private ClientConnection connection;
    private String username;

    public LoginFrame(ClientConnection connection) {
        this.connection = connection;
        initUI();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    private void initUI() {
        setTitle("登录 - 聊天系统");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        userField = new JTextField(15);
        panel.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        passField = new JPasswordField(15);
        panel.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        statusLabel = new JLabel(" ");
        panel.add(statusLabel, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        loginBtn = new JButton("登录");
        registerBtn = new JButton("注册");
        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        add(panel);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegister());
        getRootPane().setDefaultButton(loginBtn);
    }

    private void doLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空");
            return;
        }
        this.username = username;

        setComponentsEnabled(false);
        statusLabel.setText("正在登录...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new Thread(() -> {
            try {
                // ★★★ 修复：使用 Message 对象构造标准协议格式 ★★★
                // 登录消息格式：LOGIN|username||password
                Message loginMsg = new Message(Protocol.LOGIN, username, "", password);
                connection.send(loginMsg.encode());   // 发送单行

                // 接收服务器响应
                String response = connection.receive();

                SwingUtilities.invokeLater(() -> {
                    try {
                        // 根据服务器返回内容判断（可能返回 "SUCCESS|登录成功" 或直接文本）
                        // 这里保持原有逻辑，若返回以“登录成功”或“欢迎”开头则视为成功
                        if (response != null && (response.startsWith("登录成功") || response.startsWith("欢迎") || response.contains("SUCCESS"))) {
                            ChatWindow window = new ChatWindow(connection, username);
                            ClientGUI.setCurrentWindow(window);
                            window.setVisible(true);
                            ChatClient.startReceiver();
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(LoginFrame.this,
                                    "登录失败: " + (response == null ? "无响应" : response));
                            setComponentsEnabled(true);
                            statusLabel.setText(" ");
                            setCursor(Cursor.getDefaultCursor());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(LoginFrame.this, "UI更新异常: " + ex.getMessage());
                        setComponentsEnabled(true);
                        statusLabel.setText(" ");
                        setCursor(Cursor.getDefaultCursor());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "连接服务器失败: " + e.getMessage());
                    setComponentsEnabled(true);
                    statusLabel.setText(" ");
                    setCursor(Cursor.getDefaultCursor());
                });
            }
        }).start();
    }

    private void doRegister() {
        new RegisterDialog(this, connection).setVisible(true);
    }

    private void setComponentsEnabled(boolean enabled) {
        userField.setEnabled(enabled);
        passField.setEnabled(enabled);
        loginBtn.setEnabled(enabled);
        registerBtn.setEnabled(enabled);
    }
}