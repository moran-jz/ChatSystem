package client;

import javax.swing.SwingUtilities;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9000;

    private static Socket socket;
    private static ClientConnection connection;
    private static ExecutorService receiverExecutor;
    private static volatile boolean running = false; // 接收线程运行标志

    public static void main(String[] args) {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            connection = new ClientConnection(socket);

            // 启动 GUI（登录界面）
            SwingUtilities.invokeLater(() -> new LoginFrame(connection).setVisible(true));

            // 注册关闭钩子，确保资源释放
            Runtime.getRuntime().addShutdownHook(new Thread(ChatClient::shutdown));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 登录成功后调用，启动接收线程
     */
    public static void startReceiver() {
        if (running) return; // 防止重复启动
        running = true;
        receiverExecutor = Executors.newSingleThreadExecutor();
        receiverExecutor.execute(() -> {
            try {
                while (running && connection.isConnected()) {
                    String msg = connection.receive();
                    System.out.println("Server: " + msg);
                    ClientGUI.onMessage(msg);
                }
            } catch (Exception e) {
                if (running) {
                    e.printStackTrace();
                    ClientGUI.onMessage("[系统] 与服务器断开连接");
                }
            } finally {
                running = false;
            }
        });
    }

    /**
     * 关闭资源（窗口关闭时调用）
     */
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