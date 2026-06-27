package client;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    public LoginFrame(ClientConnection connection) {

        setTitle("Login");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JButton btn = new JButton("Login (test)");

        btn.addActionListener(e -> {
            connection.send("LOGIN|Tom||123456");
        });

        add(btn, BorderLayout.CENTER);
    }
}