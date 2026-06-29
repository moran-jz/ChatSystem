package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import security.SecurityBootstrap;

public class ChatServer {
    private static final int PORT = 9000;
    private static ChatServer instance;

    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = true;

    private MessageRouter router;

    // 业务线程池：用于处理路由、逻辑运算等耗时操作
    private ExecutorService businessExecutor;

    private ChatServer() throws IOException {
        this.router = new MessageRouter();
        // 创建线程池：核心线程数为 CPU 核心数，可根据需要调整
        this.businessExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
    }

    public static ChatServer getInstance() {
        return instance;
    }

    public ExecutorService getBusinessExecutor() {
        return businessExecutor;
    }

    public boolean kickUser(String username)
    {
        if(OnlineUserManager.kick(username))
        {
            return true;
        }
        return false;
    }

    public void broadcast(String msg)
    {
        OnlineUserManager.broadcast(msg);
    }

    public void groupBroadcast(String username,String msg)
    {
        OnlineUserManager.groupBroadcast(username, msg);
    }

    public boolean banUser(String username)
    {
        if(OnlineUserManager.ban(username))
        {
            return true;
        }
        return false;
    }

    public boolean unbanUser(String username)
    {
        if(OnlineUserManager.unban(username))
        {
            return true;
        }
        return false;
    }

    public Set<String> getOnlineUsers()
    {
        return OnlineUserManager.getUsersList();
    }

    public void shutdown()
    {

    }


    public void start() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("ChatServer started on port " + PORT);

        while (running) {
            try {
                if (selector.select() == 0) continue;
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    handleKey(key);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleKey(SelectionKey key) throws IOException {
        if (!key.isValid()) return;

        if (key.isAcceptable()) {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel sc = ssc.accept();
            if (sc != null) {
                sc.configureBlocking(false);
                NioClientSession session = new NioClientSession(sc, this);
                sc.register(selector, SelectionKey.OP_READ, session);
                System.out.println("New connection from " + sc.getRemoteAddress());
            }
        } else if (key.isReadable()) {
            NioClientSession session = (NioClientSession) key.attachment();
            session.handleRead();  // 该方法内会将业务逻辑提交到线程池
        } else if (key.isWritable()) {
            NioClientSession session = (NioClientSession) key.attachment();
            session.handleWrite();
            if (session.writeQueue.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    // 暴露给其他组件的 getter
    public MessageRouter getRouter() { return router; }
    public Selector getSelector() { return selector; }

    public void stop() {
        running = false;
        businessExecutor.shutdown();
        try {
            if (selector != null) selector.close();
            if (serverChannel != null) serverChannel.close();
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) throws IOException {
        instance = new ChatServer();
        // 初始化其他模块（此时 instance 已赋值）
        SecurityBootstrap.init();
        ExtensionManager.init();
        instance.start();
    }
}