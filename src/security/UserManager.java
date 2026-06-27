package security;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器（单例）。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>用户注册：密码使用 SHA-256 + 随机盐存储（委托 {@link PasswordUtil}）。</li>
 *   <li>用户登录：从存储中取出盐和哈希，对输入密码加盐后比对。</li>
 *   <li>在线状态管理：记录 / 查询当前在线用户。</li>
 *   <li>数据持久化：启动时从 {@code users.dat} 加载，注册成功后自动保存。</li>
 * </ul>
 *
 * <h3>线程安全</h3>
 * <p>
 * 内部使用 {@link ConcurrentHashMap}，所有公开方法均可安全并发调用。
 * </p>
 *
 * <h3>调用方</h3>
 * <p>
 * 由 {@link SecurityBootstrap#init()} 初始化，由 {@link AuthService} 和
 * {@code ClientHandler} 通过 {@link #getInstance()} 调用。
 * </p>
 */
public class UserManager {

    // ==================== 单例 ====================

    private static final UserManager INSTANCE = new UserManager();

    private UserManager() {
        // 私有构造，防止外部实例化
    }

    /**
     * 获取 UserManager 唯一实例。
     *
     * @return 单例实例
     */
    public static UserManager getInstance() {
        return INSTANCE;
    }

    // ==================== 内部状态 ====================

    /**
     * 用户持久化存储。
     * key   = 用户名（小写，不区分大小写）
     * value = "salt:hash" 格式的密码存储值
     */
    private final Map<String, String> userStore = new ConcurrentHashMap<>();

    /**
     * 在线用户集合。
     * 登录成功后加入，退出 / 断线后移除。
     */
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    /** 用户数据文件路径 */
    private static final String USER_DATA_FILE = "users.dat";

    // ==================== 认证 API ====================

    /**
     * 注册新用户。
     *
     * <h3>处理流程</h3>
     * <ol>
     *   <li>校验用户名和密码非空。</li>
     *   <li>检查用户名是否已存在。</li>
     *   <li>使用 SHA-256 + 随机盐对密码做哈希。</li>
     *   <li>存入内存并持久化到文件。</li>
     * </ol>
     *
     * @param username 用户名（不区分大小写）
     * @param password 明文密码
     * @return {@link AuthResult#SUCCESS} 成功；
     *         {@link AuthResult#USER_ALREADY_EXISTS} 用户已存在；
     *         {@link AuthResult#INVALID_CREDENTIALS} 用户名或密码为空
     */
    public AuthResult register(String username, String password) {
        // 1. 参数校验
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return AuthResult.INVALID_CREDENTIALS;
        }
        String key = username.trim().toLowerCase();

        // 2. 重复检查
        if (userStore.containsKey(key)) {
            return AuthResult.USER_ALREADY_EXISTS;
        }

        // 3. 加盐哈希
        String stored = PasswordUtil.hash(password);

        // 4. 存入内存
        userStore.put(key, stored);

        // 5. 持久化
        saveUsers();

        return AuthResult.SUCCESS;
    }

    /**
     * 用户登录校验。
     *
     * <h3>处理流程</h3>
     * <ol>
     *   <li>校验用户名和密码非空。</li>
     *   <li>查找用户是否存在。</li>
     *   <li>检查用户是否已在线（防止重复登录）。</li>
     *   <li>取出盐+哈希，对输入密码加盐后比对。</li>
     *   <li>比对成功则标记为在线。</li>
     * </ol>
     *
     * @param username 用户名
     * @param password 明文密码
     * @return {@link AuthResult#SUCCESS} 登录成功；
     *         {@link AuthResult#USER_NOT_FOUND} 用户不存在；
     *         {@link AuthResult#WRONG_PASSWORD} 密码错误；
     *         {@link AuthResult#USER_ALREADY_ONLINE} 用户已在线；
     *         {@link AuthResult#INVALID_CREDENTIALS} 用户名或密码为空
     */
    public AuthResult login(String username, String password) {
        // 1. 参数校验
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return AuthResult.INVALID_CREDENTIALS;
        }
        String key = username.trim().toLowerCase();

        // 2. 存在性检查
        String stored = userStore.get(key);
        if (stored == null) {
            return AuthResult.USER_NOT_FOUND;
        }

        // 3. 在线检查
        if (onlineUsers.contains(key)) {
            return AuthResult.USER_ALREADY_ONLINE;
        }

        // 4. 密码验证
        if (!PasswordUtil.verify(password, stored)) {
            return AuthResult.WRONG_PASSWORD;
        }

        // 5. 标记在线
        onlineUsers.add(key);

        return AuthResult.SUCCESS;
    }

    // ==================== 在线状态 API ====================

    /**
     * 判断用户是否在线。
     *
     * @param username 用户名
     * @return true 表示用户当前在线
     */
    public boolean isUserOnline(String username) {
        if (username == null) {
            return false;
        }
        return onlineUsers.contains(username.trim().toLowerCase());
    }

    /**
     * 将用户标记为在线（外部调用，如 ClientHandler 在登录成功后调用）。
     *
     * @param username 用户名
     */
    public void setUserOnline(String username) {
        if (username != null && !username.trim().isEmpty()) {
            onlineUsers.add(username.trim().toLowerCase());
        }
    }

    /**
     * 将用户标记为离线（在断线 / 主动退出时调用）。
     *
     * @param username 用户名
     */
    public void setUserOffline(String username) {
        if (username != null) {
            onlineUsers.remove(username.trim().toLowerCase());
        }
    }

    /**
     * 获取当前所有在线用户的快照。
     *
     * @return 在线用户名集合（不可修改）
     */
    public Set<String> getOnlineUsers() {
        return Set.copyOf(onlineUsers);
    }

    /**
     * 获取当前在线用户数。
     *
     * @return 在线人数
     */
    public int getOnlineCount() {
        return onlineUsers.size();
    }

    /**
     * 判断用户是否已注册。
     *
     * @param username 用户名
     * @return true 表示用户存在
     */
    public boolean isUserRegistered(String username) {
        if (username == null) {
            return false;
        }
        return userStore.containsKey(username.trim().toLowerCase());
    }

    // ==================== 持久化 ====================

    /**
     * 从 {@code users.dat} 文件加载用户数据到内存。
     * <p>
     * 文件格式：每行 {@code username:salt:hash}<br>
     * 注意 salt:hash 中间有冒号，因此解析时需根据第一个冒号切出 username，
     * 剩余部分为 password stored value。
     * </p>
     * <p>
     * 文件不存在或格式错误时不会抛出异常，仅输出警告并继续运行。
     * </p>
     */
    public void loadUsers() {
        Path path = Paths.get(USER_DATA_FILE);
        if (!Files.exists(path)) {
            System.out.println("[UserManager] users.dat 不存在，将使用空用户库启动。");
            return;
        }

        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // 跳过空行和注释行
                }

                // 格式: username:salt:hash
                int firstColon = line.indexOf(':');
                if (firstColon <= 0) {
                    System.err.println("[UserManager] 跳过格式错误的行: " + line);
                    continue;
                }

                String username = line.substring(0, firstColon).toLowerCase();
                String storedValue = line.substring(firstColon + 1); // salt:hash

                userStore.put(username, storedValue);
                count++;
            }
        } catch (IOException e) {
            System.err.println("[UserManager] 加载用户数据失败: " + e.getMessage());
        }

        System.out.println("[UserManager] 已加载 " + count + " 个用户。");
    }

    /**
     * 将当前内存中的用户数据保存到 {@code users.dat}。
     * <p>
     * 文件格式：每行 {@code username:salt:hash}
     * </p>
     */
    public synchronized void saveUsers() {
        Path path = Paths.get(USER_DATA_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            for (Map.Entry<String, String> entry : userStore.entrySet()) {
                // 格式: username:salt:hash
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("[UserManager] 保存用户数据失败: " + e.getMessage());
        }
    }
}
