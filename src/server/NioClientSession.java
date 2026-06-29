package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NioClientSession {
    private static final int BUFFER_SIZE = 4096;

    private final SocketChannel channel;
    final ChatServer server;  // package-private，供 MessageRouter 访问
    private final ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();

    private String username;

    public NioClientSession(SocketChannel channel, ChatServer server) {
        this.channel = channel;
        this.server = server;
    }

    /**
     * 读取通道数据，解码成完整消息后提交到业务线程池处理。
     */
    public void handleRead() throws IOException {
        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            disconnect();
            return;
        }

        readBuffer.flip();
        MessageCodec codec = new MessageCodec();
        String message;
        while ((message = codec.decode(readBuffer)) != null) {
            // 将消息处理提交到业务线程池，避免阻塞 Selector 线程
            final String msg = message;
            server.getBusinessExecutor().submit(() -> {
                server.getRouter().route(this,this.getUsername(), msg);
            });
        }
        readBuffer.compact();
    }

    public void handleWrite() throws IOException {
        ByteBuffer buf;
        while ((buf = writeQueue.peek()) != null) {
            channel.write(buf);
            if (buf.hasRemaining()) {
                return;
            }
            writeQueue.poll();
        }
    }

    public void enqueueWrite(String message) {
        byte[] bytes = (message + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        writeQueue.offer(buf);
        SelectionKey key = channel.keyFor(server.getSelector());
        if (key != null) {
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            server.getSelector().wakeup();
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public boolean isLoggedIn() {
        return username != null && !username.isEmpty();
    }

    public void disconnect() 
    {
        try {
            if (username != null) {
                OnlineUserManager.removeUser(username);
                System.out.println("User " + username + " disconnected.");
                username = null; // 避免重复清理
            }
            channel.close();
    } catch (IOException ignored) {}
}
}