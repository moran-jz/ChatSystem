
# Multi-Threaded Chat System Project Plan

---

# 1. System Overview

This project implements a multi-threaded client-server chat application with:
- TCP-based chat system
- User authentication (SHA-256 + salt)
- GUI client (Swing / JavaFX)
- HTTP file sharing (HttpURLConnection)
- UDP broadcast messaging
- Optional SSL/TLS security
- Logging + admin commands

**We have 2 versions of implementation for this system (NIO & BIO).**

---

# 2. High-Level Architecture

```
Client GUI (Swing/JavaFX)
        |
        | TCP / SSL Socket
        |
ChatServer (Multi-threaded Core)
        |
------------------------------------------------
|            |              |                  |
Auth      MessageRouter   File Server      UDP Broadcast
(B)            (A)             (D)              (D)
```

---

# 3. Development Phases

## Phase 1: Core Skeleton (A)
- Build TCP Server
- ClientHandler (multi-threaded)
- Message protocol design
- Basic message routing (private/group)
- Online user management

## Phase 2: Security (B)
- User registration/login
- SHA-256 + salt password hashing
- Session validation
- SSL/TLS integration (optional upgrade)

## Phase 3: Chat Completion (A)
- Private chat
- Group chat
- Command system (/online /msg)

## Phase 4: GUI Client (C)
- Swing/JavaFX UI
- Login window
- Chat window
- Message send/receive integration

## Phase 5: File System (D)
- HTTP file server (FileHttpServer)
- HttpURLConnection client (FileClient)
- File upload/download

## Phase 6: Extensions (D)
- UDP broadcast server
- Logging system
- Admin commands (/kick /ban)

---

# 4. Team Division

## A - Core Server
- ChatServer (main logic)
- ClientHandler
- MessageRouter
- OnlineUserManager

## B - Security
- UserManager
- Authentication service
- Password encryption
- SSL setup

## C - Client GUI
- Swing/JavaFX UI
- Chat window
- Login window
- Message display & input

## D - Extensions
- HTTP file server/client
- UDP broadcast
- Logger system
- Admin commands

---

# 5. Communication Protocol

```
TYPE|sender|receiver|content
```

Examples:
```
LOGIN|Tom||123456
PRIVATE|Tom|Jerry|Hello
GROUP|Tom|ALL|Hi
FILE|Tom|Jerry|file.jpg
```

---

# 6. SERVER MAIN (ChatServer.java)

```java
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {

    private static final int PORT = 9000;

    public static void main(String[] args) {
        try {
            // Init security module (B)
            SecurityBootstrap.init();

            // Init extensions (D)
            ExtensionManager.init();

            // Start server core (A)
            startServer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startServer() throws Exception {

        ServerSocket serverSocket = new ServerSocket(PORT);

        System.out.println("Chat Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();

            ClientHandler handler = new ClientHandler(clientSocket);
            handler.start();
        }
    }
}
```

---

# 7. CLIENT MAIN (ChatClient.java)

```java
import javax.swing.SwingUtilities;
import java.net.Socket;

public class ChatClient {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9000;

    private static Socket socket;
    private static ClientConnection connection;

    public static void main(String[] args) {

        try {
            // Connect to server
            socket = new Socket(SERVER_IP, SERVER_PORT);
            connection = new ClientConnection(socket);

            // Start GUI
            SwingUtilities.invokeLater(() -> {
                new LoginFrame(connection).setVisible(true);
            });

            // Start listening thread
            new Thread(() -> listenServer()).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listenServer() {
        try {
            while (true) {
                String msg = connection.receive();

                System.out.println("Server: " + msg);

                GUIMessageBus.onMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

# 8. Design Principle

- Server = Core engine (A)
- Security = gatekeeper (B)
- GUI = interface layer (C)
- Extensions = plugin modules (D)

---

# 9. Advanced Step

You can extend:
- ClientHandler logic
- GUI implementation
- HTTP file transfer module
- UDP broadcast module
- Security module
