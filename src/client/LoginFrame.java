package client;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private ClientConnection connection;

    public LoginFrame(ClientConnection connection) {
        this.connection = connection;
        setTitle("登录");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 使用 GridBagLayout 更灵活，但保持简洁
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("用户名:"));
        userField = new JTextField("admin");
        panel.add(userField);

        panel.add(new JLabel("密码:"));
        passField = new JPasswordField("123456");
        panel.add(passField);

        JButton loginBtn = new JButton("登录");
        loginBtn.addActionListener(e -> doLogin());
        panel.add(loginBtn);

        JButton registerBtn = new JButton("注册");
        registerBtn.addActionListener(e -> openRegisterDialog());
        panel.add(registerBtn);

        JButton quitBtn = new JButton("退出");
        quitBtn.addActionListener(e -> System.exit(0));
        panel.add(quitBtn);

        // 调整布局：登录和注册一行，退出单独占一行（但 GridLayout 2列，所以会占一行）
        // 这里我们简单添加一个空白占位让退出居中
        // 也可以使用其他布局，但为了简单，我们只添加三个按钮，剩余位置留给空白
        // 由于 GridLayout 是 4 行 2 列，已有 7 个组件（3标签+3按钮+空白占位），再补一个占位
        panel.add(new JLabel("")); // 占位

        add(panel, BorderLayout.CENTER);
        pack();
    }

    private void doLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空");
            return;
        }

        try {
            // **新协议**：先发送操作类型 "LOGIN"
            connection.send("LOGIN");
            connection.send(username);
            connection.send(password);

            // 等待服务器响应
            String response = connection.receive();
            System.out.println("登录响应: " + response);

            if (response.startsWith("登录成功") || response.startsWith("欢迎")) {
                // 启动接收线程
                ChatClient.startReceiver();

                // 跳转到聊天窗口
                SwingUtilities.invokeLater(() -> {
                    new ChatWindow(connection, username).setVisible(true);
                    dispose(); // 关闭登录窗口
                });
            } else {
                JOptionPane.showMessageDialog(this, "登录失败: " + response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "连接服务器失败: " + e.getMessage());
        }
    }

    /**
     * 打开注册对话框
     */
    private void openRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this, connection);
        dialog.setVisible(true);
        // 注册成功后，对话框会调用 setUsername 方法填充用户名到登录框
    }

    /**
     * 供 RegisterDialog 调用的方法，自动填入用户名
     */
    public void setUsername(String username) {
        userField.setText(username);
        passField.setText(""); // 清空密码框，让用户手动输入密码
    }
}