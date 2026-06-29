package server;

public class MessageRouter {

    public void route(NioClientSession session, String raw) {
        if (raw == null || raw.isEmpty()) return;

        String[] parts = raw.split("\\|", 4);
        if (parts.length < 2) {
            session.enqueueWrite("ERROR|SYSTEM||Invalid message format");
            return;
        }

        String type = parts[0].toUpperCase();
        String sender = parts.length > 1 ? parts[1] : "";
        String receiver = parts.length > 2 ? parts[2] : "";
        String content = parts.length > 3 ? parts[3] : "";

        switch (type) {
            case "LOGIN":
                handleLogin(session, sender, content);
                break;
            case "PRIVATE":
                handlePrivate(sender, receiver, content);
                break;
            case "GROUP":
                handleGroup(sender, content);
                break;
            case "FILE":
                handleFile(sender, receiver, content);
                break;
            default:
                session.enqueueWrite("ERROR|SYSTEM||Unknown command type");
        }
    }

    private void handleLogin(NioClientSession session, String username, String password) {
        // 这里应调用 SecurityBootstrap.authenticate(username, password)
        boolean ok = true; // 临时允许
        if (ok) {
            NioClientSession old = OnlineUserManager.getSession(username);
            if (old != null) {
                old.enqueueWrite("SYSTEM|||You are kicked by another login");
                old.disconnect();
            }
            session.setUsername(username);
            OnlineUserManager.addUser(username, session);
            session.enqueueWrite("LOGIN_OK|SYSTEM||Welcome " + username);
            System.out.println("User " + username + " logged in.");
        } else {
            session.enqueueWrite("LOGIN_FAIL|SYSTEM||Invalid credentials");
        }
    }

    private void handlePrivate(String sender, String receiver, String content) {
        if (!OnlineUserManager.isOnline(receiver)) {
            NioClientSession senderSession = OnlineUserManager.getSession(sender);
            if (senderSession != null) {
                senderSession.enqueueWrite("ERROR|SYSTEM||User " + receiver + " is offline");
            }
            return;
        }
        String msg = "PRIVATE|" + sender + "|" + receiver + "|" + content;
        OnlineUserManager.sendToUser(receiver, msg);
    }

    private void handleGroup(String sender, String content) {
        String msg = "GROUP|" + sender + "|ALL|" + content;
        OnlineUserManager.groupBroadcast(sender, msg);
    }

    private void handleFile(String sender, String receiver, String filename) {
        String notice = "FILE_REQUEST|" + sender + "|" + receiver + "|" + filename;
        OnlineUserManager.sendToUser(receiver, notice);
    }
}