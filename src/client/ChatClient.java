package client;

import javax.swing.*;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9000;

    private static ClientConnection connection;
    private static ExecutorService receiverExecutor;
    private static volatile boolean running = false;
    
    public static void main(String[] args) {
        try {
            connection = new ClientConnection(SERVER_IP, SERVER_PORT);
            SwingUtilities.invokeLater(() -> new LoginFrame(connection).setVisible(true));
            Runtime.getRuntime().addShutdownHook(new Thread(ChatClient::shutdown));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("无法连接到服务器，请确认服务器已启动");
        }
    }

    public static ClientConnection getConnection() {
    return connection;
    }
    public static void startReceiver() {
        if (running) return;
        running = true;
        receiverExecutor = Executors.newSingleThreadExecutor();
        receiverExecutor.execute(() -> {
            try {
                while (running && connection.isConnected()) {
                    try {
                        String msg = connection.receive();
                        if (msg == null) {
                            // 对端关闭连接
                            System.out.println("服务器关闭了连接");
                            handleDisconnect();  // ★ 新增：处理断开连接
                            break;
                        }
                        System.out.println("Server: " + msg);
                        ClientGUI.onMessage(msg);
                    } catch (SocketTimeoutException e) {
                        // 读超时是正常的，继续等待下一条消息
                        System.out.println("读取超时，继续等待...");
                    }
                }
            } catch (Exception e) {
                if (running) {
                    e.printStackTrace();
                    ClientGUI.onMessage("[系统] 与服务器断开连接");
                    handleDisconnect();  // ★ 新增：异常断开也处理
                }
            } finally {
                running = false;
            }
        });
    }

    // ★ 新增：处理服务器断开连接的方法
    private static void handleDisconnect() {
        running = false;
        SwingUtilities.invokeLater(() -> {
            // 关闭当前聊天窗口（如果有）
            ChatWindow current = ClientGUI.getCurrentWindow();
            if (current != null) {
                current.dispose();
            }
            // 显示提示并退出程序
            JOptionPane.showMessageDialog(null, "与服务器的连接已断开，程序将退出。");
            System.exit(0);
        });
    }

    public static void shutdown() {
        running = false;
        if (connection != null) {
            connection.close();
        }
        if (receiverExecutor != null) {
            receiverExecutor.shutdownNow();
            try {
                if (!receiverExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    receiverExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                receiverExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}