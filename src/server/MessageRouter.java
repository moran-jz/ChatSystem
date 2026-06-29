package server;

import security.SecurityBootstrap;
import security.UserManager;
import security.AuthResult;
import admin.AdminCommandHandler;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class MessageRouter {

    // 不再使用成员变量，改为动态获取
    // private final AdminCommandHandler adminHandler = ExtensionManager.getAdminCommandHandler();

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
            case "REGISTER":
                handleRegister(session, sender, content);
                break;
            case "PRIVATE":
                handlePrivate(session, sender, receiver, content);
                break;
            case "BROADCAST":
            case "GROUP":
                handleGroup(session, sender, content);
                break;
            case "FILE_UPLOAD":
                handleFileUpload(session, sender, receiver, content);
                break;
            case "FILE":
                handleFile(sender, receiver, content);
                break;
            default:
                session.enqueueWrite("ERROR|SYSTEM||Unknown command type");
        }
    }

    // ========== 登录 ==========
    private void handleLogin(NioClientSession session, String username, String password) {
        boolean ok = SecurityBootstrap.authenticate(username, password);
        if (ok) {
            NioClientSession old = OnlineUserManager.getSession(username);
            if (old != null) {
                old.enqueueWrite("SYSTEM|||您已在别处登录，将被踢下线");
                old.disconnect();
            }
            session.setUsername(username);
            OnlineUserManager.addUser(username, session);
            session.enqueueWrite("SUCCESS|SYSTEM||登录成功");
            System.out.println("User " + username + " logged in.");
        } else {
            session.enqueueWrite("FAIL|SYSTEM||用户名或密码错误");
        }
    }

    // ========== 注册 ==========
    private void handleRegister(NioClientSession session, String username, String password) {
        UserManager um = UserManager.getInstance();
        AuthResult result = um.register(username, password);
        if (result.isSuccess()) {
            session.enqueueWrite("SUCCESS|SYSTEM||注册成功");
            System.out.println("User " + username + " registered.");
        } else {
            session.enqueueWrite("FAIL|SYSTEM||" + result.getMessage());
        }
    }

    // ========== 私聊 ==========
    private void handlePrivate(NioClientSession session, String sender, String receiver, String content) {
        if (!OnlineUserManager.isOnline(receiver)) {
            NioClientSession senderSession = OnlineUserManager.getSession(sender);
            if (senderSession != null) {
                senderSession.enqueueWrite("ERROR|SYSTEM||用户 " + receiver + " 不在线");
            }
            return;
        }
        String msg = "PRIVATE|" + sender + "|" + receiver + "|" + content;
        OnlineUserManager.sendToUser(receiver, msg);
    }

    // ========== 群聊 / 命令 ==========
    private void handleGroup(NioClientSession session, String sender, String content) {
        if (content.startsWith("/")) {
            handleAdminCommand(session, sender, content);
            return;
        }
        String msg = "BROADCAST|" + sender + "|ALL|" + content;
        OnlineUserManager.groupBroadcast(sender, msg);
    }

    // ========== ★ 管理员命令处理（动态获取 adminHandler） ==========
    private void handleAdminCommand(NioClientSession session, String sender, String commandLine) {
        // ★ 关键修复：动态获取 AdminCommandHandler，而不是使用成员变量
        AdminCommandHandler adminHandler = ExtensionManager.getAdminCommandHandler();
        if (adminHandler == null) {
            session.enqueueWrite("SYSTEM|||管理员模块未初始化");
            return;
        }

        String[] tokens = commandLine.split("\\s+");
        String cmd = tokens[0].toLowerCase();
        String result = "";

        try {
            switch (cmd) {
                case "/list":
                    result = "在线用户: " + OnlineUserManager.getUsersList().toString();
                    break;
                case "/help":
                    result = "命令列表: /list, /kick <user>, /ban <user>, /unban <user>, /download <filename>";
                    break;
                case "/kick":
                    if (tokens.length < 2) result = "用法: /kick <用户名>";
                    else {
                        boolean kicked = ChatServer.getInstance().kickUser(tokens[1]);
                        result = kicked ? "已踢出用户: " + tokens[1] : "用户不存在或不在线";
                    }
                    break;
                case "/ban":
                    if (tokens.length < 2) result = "用法: /ban <用户名>";
                    else {
                        boolean banned = ChatServer.getInstance().banUser(tokens[1]);
                        result = banned ? "已封禁用户: " + tokens[1] : "用户不存在或不在线";
                    }
                    break;
                case "/unban":
                    if (tokens.length < 2) result = "用法: /unban <用户名>";
                    else {
                        boolean unbanned = ChatServer.getInstance().unbanUser(tokens[1]);
                        result = unbanned ? "已解封用户: " + tokens[1] : "用户未被封禁";
                    }
                    break;
                case "/download":
                    if (tokens.length < 2) result = "用法: /download <文件名>";
                    else {
                        String encoded = URLEncoder.encode(tokens[1], "UTF-8");
                        result = "文件下载链接: http://127.0.0.1:8080/files/" + encoded + " (请复制到浏览器下载)";
                    }
                    break;
                default:
                    result = "未知命令，输入 /help 查看帮助";
            }
        } catch (Exception e) {
            result = "执行命令出错: " + e.getMessage();
        }
        // 将结果私聊返回给命令发起者
        session.enqueueWrite("PRIVATE|SYSTEM|" + sender + "|" + result);
    }

    // ========== 文件上传 ==========
    private void handleFileUpload(NioClientSession session, String sender, String receiver, String data) {
        try {
            int colonPos = data.indexOf(':');
            if (colonPos == -1) {
                session.enqueueWrite("ERROR|SYSTEM||无效的文件上传格式");
                return;
            }
            String encodedFilename = data.substring(0, colonPos);
            String base64 = data.substring(colonPos + 1);

            String filename = URLDecoder.decode(encodedFilename, StandardCharsets.UTF_8.name());
            base64 = base64.replaceAll("\\s", "");

            byte[] fileBytes;
            try {
                fileBytes = Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException e) {
                session.enqueueWrite("ERROR|SYSTEM||Base64解码失败: " + e.getMessage());
                return;
            }

            Path fileDir = Paths.get("server_files");
            if (!Files.exists(fileDir)) Files.createDirectories(fileDir);
            String safeFilename = new File(filename).getName();
            Path filePath = fileDir.resolve(safeFilename);
            Files.write(filePath, fileBytes);

            session.enqueueWrite("SUCCESS|SYSTEM||文件上传成功: " + safeFilename);

            String encodedUrlFilename = URLEncoder.encode(safeFilename, StandardCharsets.UTF_8.name());
            String downloadUrl = "http://127.0.0.1:8080/files/" + encodedUrlFilename;
            String broadcastMsg = "BROADCAST|SYSTEM|ALL|用户 " + sender + " 上传了文件: " + safeFilename +
                    " (下载: " + downloadUrl + ")";
            OnlineUserManager.broadcast(broadcastMsg);

            System.out.println("文件上传: " + safeFilename + " 来自 " + sender);

        } catch (Exception e) {
            e.printStackTrace();
            session.enqueueWrite("ERROR|SYSTEM||文件上传失败: " + e.getMessage());
        }
    }

    // ========== 文件请求 ==========
    private void handleFile(String sender, String receiver, String filename) {
        try {
            String encoded = URLEncoder.encode(filename, "UTF-8");
            String notice = "FILE_REQUEST|" + sender + "|" + receiver + "|" + encoded;
            OnlineUserManager.sendToUser(receiver, notice);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}