package udp;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import server.ChatServer;
import log.Logger;
/**
 * UDP 广播服务器：监听 UDP 端口，接收来自客户端的广播消息，
 * 然后将消息转发给所有在线 TCP 客户端（通过 ChatServer 的广播方法）。
 * 需要 ChatServer 提供 public void broadcastToAll(String message) 方法。
 */
public class UDPBroadcastServer implements Runnable {
    private static final int UDP_PORT = 9999;
    private final ChatServer chatServer;  // 假设 ChatServer 有 broadcastToAll 方法
    private DatagramSocket socket;
    private volatile boolean running = true;

    /**
     * 构造时传入 ChatServer 实例，以便调用其广播方法。
     * @param chatServer 主服务器实例
     */
    public UDPBroadcastServer(ChatServer chatServer) {
        this.chatServer = chatServer;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(UDP_PORT);
            Logger.getInstance().info("UDP Broadcast Server started on port " + UDP_PORT);
            byte[] buffer = new byte[4096];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                Logger.getInstance().debug("UDP received: " + message);

                // 将收到的消息通过 TCP 广播给所有在线用户
                if (chatServer != null) {
                    chatServer.broadcast("[UDP广播] " + message);
                } else {
                    Logger.getInstance().warning("ChatServer reference is null, cannot broadcast.");
                }
            }
        } catch (SocketException e) {
            if (!running) {
                Logger.getInstance().info("UDP socket closed normally.");
            } else {
                Logger.getInstance().error("UDP Socket error: " + e.getMessage());
            }
        } catch (IOException e) {
            Logger.getInstance().error("UDP receive error: " + e.getMessage());
        } finally {
            close();
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        Logger.getInstance().info("UDP Broadcast Server stopped.");
    }

    private void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    // 注意：ChatServer 需要实现类似以下的方法
    // public void broadcastToAll(String message) { ... }
}