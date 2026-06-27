package client;

import javax.swing.*;
import java.awt.*;

public class RegisterDialog extends JDialog {
    private JTextField userField;
    private JPasswordField passField;
    private JPasswordField confirmField;
    private ClientConnection connection;
    private JFrame parent;

    public RegisterDialog(JFrame parent, ClientConnection connection) {
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

        try {
            connection.send("REGISTER");
            connection.send(username);
            connection.send(password);

            String response = connection.receive();
            JOptionPane.showMessageDialog(this, response);
            if (response.startsWith("注册成功")) {
                if (parent instanceof LoginFrame) {
                    ((LoginFrame) parent).setUsername(username);
                }
            }
            // 注册完成后主动关闭连接（告知服务器断开）
            connection.close();
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "注册请求失败: " + e.getMessage());
        }
    }

}