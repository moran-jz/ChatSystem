package server;

import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // 占位：处理客户端连接（不实现具体逻辑）
    }
}
