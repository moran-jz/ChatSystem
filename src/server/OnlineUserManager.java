package server;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineUserManager {
    private static final Map<String, NioClientSession> onlineUsers = new ConcurrentHashMap<>();
    private static final Set<String> bannedList=new HashSet<>();

    public static void addUser(String username, NioClientSession session) {
        onlineUsers.put(username, session);
    }

    public static void removeUser(String username) {
        onlineUsers.remove(username);
    }

    public static NioClientSession getSession(String username) {
        return onlineUsers.get(username);
    }

    public static boolean isOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public static void sendToUser(String username, String message) {
        NioClientSession session = onlineUsers.get(username);
        if (session != null) {
            session.enqueueWrite(message);
        }
    }

    public static String getOnlineList() {
        return String.join(",", onlineUsers.keySet());
    }

    public static void groupBroadcast(String Username, String msg) {
        for (Map.Entry<String, NioClientSession> entry : onlineUsers.entrySet()) {
            if (!entry.getKey().equals(Username)) {
                entry.getValue().enqueueWrite(msg);
            }
        }
    }

    public static void broadcast(String msg)
    {
        for (Map.Entry<String, NioClientSession> entry : onlineUsers.entrySet()) 
        {
            entry.getValue().enqueueWrite(msg);
        }
    }
    
    public static boolean kick(String username)
    {
        if(isOnline(username))
        {
            onlineUsers.remove(username);
            return true;
        }
        return false;
    }

    public static boolean isBanned(String username)
    {
        if(bannedList.contains(username))
        {
            return true;
        }
        return false;
    }

    public static boolean ban(String username)
    {
        if(isOnline(username))
        {
            onlineUsers.remove(username);
            bannedList.add(username);
            return true;
        }
        return false;
    }

    public static boolean unban(String username)
    {
        if(isBanned(username))
        {
            bannedList.remove(username);
            return true;
        }
        return false;
    }

    public static Set<String> getUsersList()
    {
        return onlineUsers.keySet();
    }





}