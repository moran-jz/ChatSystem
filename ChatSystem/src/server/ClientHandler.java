package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler  {
    private final Socket socket;
    ExecutorService pool;
    public ClientHandler(Socket socket) {
        this.socket = socket;
        pool = Executors.newFixedThreadPool(10);
    }

    public void start() {
        // 占位：处理客户端连接（不实现具体逻辑）
        pool.execute(() -> 
        {
            try(DataOutputStream dos=new DataOutputStream(socket.getOutputStream());
                DataInputStream dis=new DataInputStream(socket.getInputStream());)
            {
                while(true)
                {
                    String line=dis.readUTF();
                    System.out.println(line);
                }

            }
            catch(IOException e)
            {
                System.out.println(socket.getRemoteSocketAddress()+" disconnected.");
                //写入日志
                Log.add(socket.getRemoteSocketAddress()+" disconnected.");
            }
        });
    }
}
