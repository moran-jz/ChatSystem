package security;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Set;

public class UserManager {
    private static UserManager instance;
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final Set<String> onlineUsers = new HashSet<>();
    private final String USER_DATA_FILE = "users.dat";

    private UserManager() {
        loadUsers();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // ---------- 数据持久化 ----------
    public void loadUsers() {
        Path path = Paths.get(USER_DATA_FILE);
        if (!Files.exists(path)) {
            System.out.println("[UserManager] users.dat 不存在，将使用空用户库启动。");
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":", 3);
                if (parts.length == 3) {
                    users.put(parts[0], new User(parts[0], parts[1], parts[2]));
                }
            }
            System.out.println("[UserManager] 已加载 " + users.size() + " 个用户。");
        } catch (IOException e) {
            System.err.println("[UserManager] 加载用户数据出错: " + e.getMessage());
        }
    }

    public void saveUsers() {
        Path path = Paths.get(USER_DATA_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (User user : users.values()) {
                writer.write(user.username + ":" + user.salt + ":" + user.passwordHash);
                writer.newLine();
            }
            System.out.println("[UserManager] 用户数据已保存至 " + USER_DATA_FILE);
        } catch (IOException e) {
            System.err.println("[UserManager] 保存用户数据出错: " + e.getMessage());
        }
    }

    // ---------- 注册 ----------
    public AuthResult register(String username, String password) {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return AuthResult.fail("用户名或密码不能为空");
        }
        if (users.containsKey(username)) {
            return AuthResult.fail("用户名已存在");
        }
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        users.put(username, new User(username, salt, hash));
        saveUsers();
        return AuthResult.success();
    }

    // ---------- 登录 ----------
    public AuthResult login(String username, String password) {
        if (username == null || password == null) {
            return AuthResult.fail("用户名或密码不能为空");
        }
        User user = users.get(username);
        if (user == null) {
            return AuthResult.fail("用户名不存在");
        }
        String computedHash = hashPassword(password, user.salt);
        if (!computedHash.equals(user.passwordHash)) {
            return AuthResult.fail("密码错误");
        }
        onlineUsers.add(username);
        return AuthResult.success();
    }

    // ---------- 设置离线 ----------
    public void setUserOffline(String username) {
        if (username != null) {
            onlineUsers.remove(username);
        }
    }

    // ---------- 纯验证（不改变在线状态） ----------
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;
        User user = users.get(username);
        if (user == null) return false;
        return hashPassword(password, user.salt).equals(user.passwordHash);
    }

    // ---------- 查询在线用户 ----------
    public Set<String> getOnlineUsers() {
        return new HashSet<>(onlineUsers);
    }

    // ---------- 工具 ----------
    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- 获取用户数 ----------
    public int getUserCount() {
        return users.size();
    }

    // ---------- 内部类 ----------
    private static class User {
        final String username;
        final String salt;
        final String passwordHash;
        User(String username, String salt, String passwordHash) {
            this.username = username;
            this.salt = salt;
            this.passwordHash = passwordHash;
        }
    }
}