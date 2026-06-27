package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class OnlineUserManager {
    private static final Map<String, DataOutputStream> onlineUsers = new ConcurrentHashMap<>();

    private OnlineUserManager(){}

    public static boolean isOnline(String user) 
    {
        return onlineUsers.containsKey(user);
    }

    public static int OnlineCount()
    {
        return onlineUsers.size();
    }

    public static void userLogin(String userName, DataOutputStream user) 
    {
        onlineUsers.put(userName, user);
    }

    public static void userLogout(String userName) 
    {
        DataOutputStream dos = onlineUsers.remove(userName);
        if (dos != null) {
            try 
            {
                dos.close();
            } 
            catch (IOException ignored) {}
        }
    }

    public static DataOutputStream getStream(String user)
    {
        return onlineUsers.get(user);
    }

    public static Set<String> getKeys()
    {
        return onlineUsers.keySet();
    }

    public static Collection<DataOutputStream> getValues()
    {
        return onlineUsers.values();
    }


}