package client;

import common.Message;
import common.Protocol;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局 UI 工具类，用于在主窗口和私聊窗口之间分发消息。
 */
public class ClientGUI {
    private static final String ONLINE_USERS_PREFIX = "ONLINE_USERS|";
    private static final Map<String, PrivateChatWindow> privateWindows = new ConcurrentHashMap<>();
    private static final List<String> pendingGroupMessages = new ArrayList<>();
    private static final Object LOCK = new Object();

    private static ChatWindow mainWindow;
    private static ClientConnection connection;
    private static String currentUser;
    private static List<String> pendingOnlineUsers = new ArrayList<>();

    public static void registerMainWindow(ChatWindow window, ClientConnection clientConnection, String username) {
        synchronized (LOCK) {
            mainWindow = window;
            connection = clientConnection;
            currentUser = username;
        }

        SwingUtilities.invokeLater(() -> {
            List<String> cachedMessages;
            List<String> cachedUsers;
            synchronized (LOCK) {
                cachedMessages = new ArrayList<>(pendingGroupMessages);
                cachedUsers = new ArrayList<>(pendingOnlineUsers);
                pendingGroupMessages.clear();
            }

            for (String msg : cachedMessages) {
                window.appendGroupMessage(msg);
            }
            if (!cachedUsers.isEmpty()) {
                window.updateOnlineUsers(cachedUsers);
            }
        });
    }

    public static void unregisterMainWindow(ChatWindow window) {
        synchronized (LOCK) {
            if (mainWindow == window) {
                mainWindow = null;
                connection = null;
                currentUser = null;
                pendingOnlineUsers = new ArrayList<>();
            }
        }

        SwingUtilities.invokeLater(() -> {
            for (PrivateChatWindow privateWindow : privateWindows.values()) {
                privateWindow.dispose();
            }
            privateWindows.clear();
        });
    }

    public static void unregisterPrivateChat(String targetUser) {
        privateWindows.remove(targetUser);
    }

    public static void openPrivateChat(String targetUser) {
        if (targetUser == null || targetUser.trim().isEmpty()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            synchronized (LOCK) {
                if (connection == null || currentUser == null || currentUser.equals(targetUser)) {
                    return;
                }
            }
            getOrCreatePrivateWindow(targetUser);
        });
    }

    public static void onMessage(String msg) {
        Message protocolMessage = Message.decode(msg);
        if (protocolMessage != null && Protocol.PRIVATE.equals(protocolMessage.getType())) {
            handlePrivateMessage(protocolMessage);
            return;
        }

        if (msg.startsWith(ONLINE_USERS_PREFIX)) {
            updateOnlineUsers(msg.substring(ONLINE_USERS_PREFIX.length()));
            return;
        }

        appendGroupMessage(msg);
    }

    private static void handlePrivateMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            synchronized (LOCK) {
                if (connection == null || currentUser == null) {
                    return;
                }
            }
            PrivateChatWindow window = getOrCreatePrivateWindow(message.getSender());
            window.appendIncomingMessage(message.getSender(), message.getContent());
        });
    }

    private static PrivateChatWindow getOrCreatePrivateWindow(String targetUser) {
        PrivateChatWindow window = privateWindows.get(targetUser);
        if (window == null) {
            window = new PrivateChatWindow(connection, currentUser, targetUser);
            privateWindows.put(targetUser, window);
        }

        if (!window.isVisible()) {
            window.setVisible(true);
        }
        window.toFront();
        window.requestFocus();
        return window;
    }

    private static void appendGroupMessage(String msg) {
        ChatWindow window;
        synchronized (LOCK) {
            window = mainWindow;
            if (window == null) {
                pendingGroupMessages.add(msg);
                return;
            }
        }

        SwingUtilities.invokeLater(() -> window.appendGroupMessage(msg));
    }

    private static void updateOnlineUsers(String usersLine) {
        List<String> users = new ArrayList<>();
        if (!usersLine.isEmpty()) {
            String[] parts = usersLine.split(",");
            LinkedHashSet<String> uniqueUsers = new LinkedHashSet<>();
            for (String part : parts) {
                String user = part.trim();
                if (!user.isEmpty()) {
                    uniqueUsers.add(user);
                }
            }
            users.addAll(uniqueUsers);
        }

        ChatWindow window;
        synchronized (LOCK) {
            window = mainWindow;
            pendingOnlineUsers = new ArrayList<>(users);
            if (window == null) {
                return;
            }
        }

        SwingUtilities.invokeLater(() -> window.updateOnlineUsers(users));
    }
}