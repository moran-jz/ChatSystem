package server;

import java.io.DataOutputStream;
import java.io.IOException;

public class MessageRouter {

    private MessageRouter() {} 

    public static void sendPrivateMessage(String sender, String target, String msg) 
    {
        if (!OnlineUserManager.isOnline(sender)) return; // 发送者不在线，直接返回

        DataOutputStream targetDos = OnlineUserManager.getStream(target);
        if (targetDos != null) 
        {
            try 
            {
                targetDos.writeUTF("[私聊] " + sender + ": " + msg);
                targetDos.flush();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        } 
        else 
        {
            // 目标不在线，通知发送者
            DataOutputStream senderDos = OnlineUserManager.getStream(sender);
            if (senderDos != null) 
                {
                try 
                {
                    senderDos.writeUTF("用户 " + target + " 不在线");
                    senderDos.flush();
                } 
                catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void broadcastMessage(String sender, String msg) 
    {
        if (!OnlineUserManager.isOnline(sender)) return;

        for (String username : OnlineUserManager.getKeys()) 
            {
            if (!username.equals(sender)) 
            {
                DataOutputStream dos = OnlineUserManager.getStream(username);
                if (dos == null) continue; 
                try 
                {
                    dos.writeUTF("[群聊] " + sender + ": " + msg);
                    dos.flush();
                } 
                catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        }
    }




}