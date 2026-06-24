package client;

import java.io.*;
import java.net.Socket;

public class ClientConnection {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void send(String msg) {
        out.println(msg);
    }

    public String receive() throws IOException {
        return in.readLine();
    }

    public void close() throws IOException {
        socket.close();
    }
}