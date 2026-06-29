package client;

import java.io.*;
import java.net.*;

public class ClientConnection {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ClientConnection(String host, int port) throws IOException {
        socket = connect(host, port);
        socket.setSoTimeout(5000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private Socket connect(String host, int port) throws IOException {
        InetAddress[] addresses = InetAddress.getAllByName(host);
        IOException lastException = null;

        for (InetAddress address : addresses) {
            Socket candidate = new Socket();
            try {
                candidate.connect(new InetSocketAddress(address, port), 5000);
                return candidate;
            } catch (IOException e) {
                lastException = e;
                try {
                    candidate.close();
                } catch (IOException ignored) {}
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new UnknownHostException("无法解析主机: " + host);
    }

    public void send(String message) throws IOException {
        if (out == null) throw new IOException("连接未建立");
        out.print(message);
        out.print('\n');
        out.flush();
    }

    public String receive() throws IOException {
        if (in == null) throw new IOException("连接未建立");
        return in.readLine();
    }

    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
