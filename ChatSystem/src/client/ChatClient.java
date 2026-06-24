package client;
import javax.swing.SwingUtilities;
import java.net.Socket;

public class ChatClient {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9000;

    private static Socket socket;
    private static ClientConnection connection;

    public static void main(String[] args) {

        try {
            // 1. 先连接服务器
            socket = new Socket(SERVER_IP, SERVER_PORT);
            connection = new ClientConnection(socket);

            // 2. 启动GUI（必须在Swing线程）
            SwingUtilities.invokeLater(() -> {
                new LoginFrame(connection).setVisible(true);
            });

            // 3. 启动消息接收线程
            new Thread(() -> listenServer()).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listenServer() {
        try {
            while (true) {
                String msg = connection.receive();

                // 把消息转发给GUI（更新聊天窗口）
                System.out.println("Server: " + msg);

                ClientGUI.onMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}