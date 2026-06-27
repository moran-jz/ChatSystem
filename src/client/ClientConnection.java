package client;

import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private volatile boolean connected = true;

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    // 发送消息（线程安全）
    public synchronized void send(String msg) throws IOException {
        if (!connected) throw new IOException("连接已关闭");
        dos.writeUTF(msg);
        dos.flush();
    }

    // 接收消息（阻塞）
    public String receive() throws IOException {
        if (!connected) throw new IOException("连接已关闭");
        return dis.readUTF();
    }

    // 关闭连接
    public synchronized void close() {
        connected = false;
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }
}