package client;

import common.Message;
import common.Protocol;

import javax.swing.*;
import java.awt.*;

public class RegisterDialog extends JDialog {
    private JTextField userField;
    private JPasswordField passField;
    private JPasswordField confirmField;
    private ClientConnection connection;
    private LoginFrame parent;  // 明确类型为 LoginFrame

    public RegisterDialog(LoginFrame parent, ClientConnection connection) {
        super(parent, "注册新用户", true);
        this.parent = parent;
        this.connection = connection;
        setSize(320, 220);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.add(new JLabel("用户名:"));
        userField = new JTextField();
        panel.add(userField);
        panel.add(new JLabel("密码:"));
        passField = new JPasswordField();
        panel.add(passField);
        panel.add(new JLabel("确认密码:"));
        confirmField = new JPasswordField();
        panel.add(confirmField);

        JButton okBtn = new JButton("注册");
        okBtn.addActionListener(e -> doRegister());
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel();
        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);

        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void doRegister() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空");
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "两次密码输入不一致");
            return;
        }

        // 禁用按钮，防止重复提交
        setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new Thread(() -> {
            try {
                // ★★★ 修正：使用 Message 构造标准协议 ★★★
                Message regMsg = new Message(Protocol.REGISTER, username, "", password);
                connection.send(regMsg.encode());

                String response = connection.receive();

                SwingUtilities.invokeLater(() -> {
                    try {
                        // 根据响应内容判断成功与否
                        boolean success = response != null &&
                                (response.contains("SUCCESS") || response.startsWith("注册成功"));
                        if (success) {
                            JOptionPane.showMessageDialog(RegisterDialog.this, "注册成功！请登录");
                            // 将用户名回填到登录框
                            parent.setUsername(username);
                            dispose();  // 关闭注册窗口
                        } else {
                            JOptionPane.showMessageDialog(RegisterDialog.this,
                                    "注册失败: " + (response == null ? "无响应" : response));
                        }
                    } finally {
                        setEnabled(true);
                        setCursor(Cursor.getDefaultCursor());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "注册请求异常: " + e.getMessage());
                    setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                });
            }
        }).start();
    }
}