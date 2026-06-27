package security;

/**
 * 安全模块启动器。
 *
 * <h3>调用方</h3>
 * <p>
 * 由 {@code ChatServer.main()} 在服务器启动的最早期调用：
 * <pre>
 *   SecurityBootstrap.init();
 * </pre>
 * </p>
 *
 * <h3>初始化顺序</h3>
 * <ol>
 *   <li>获取 {@link UserManager} 单例，触发实例化。</li>
 *   <li>从 {@code users.dat} 加载已有用户数据。</li>
 *   <li>打印初始化完成日志。</li>
 * </ol>
 *
 * <h3>后续可选步骤</h3>
 * <p>
 * 如果启用了 SSL，调用方可在 {@code init()} 之后通过
 * {@link SSLContextBuilder} 获取 {@code SSLServerSocketFactory}。
 * </p>
 */
public class SecurityBootstrap {

    private static volatile boolean initialized = false;

    /**
     * 初始化安全模块。
     * <p>
     * 此方法是幂等的：重复调用不会重新初始化。
     * </p>
     */
    public static void init() {
        if (initialized) {
            System.out.println("[SecurityBootstrap] 安全模块已初始化，跳过重复调用。");
            return;
        }
        initialized = true;

        // 1. 初始化用户管理器（触发单例实例化）
        UserManager userManager = UserManager.getInstance();

        // 2. 加载持久化的用户数据
        userManager.loadUsers();

        // 3. 打印初始化完成日志
        System.out.println("Security module initialized.");
        System.out.println("[SecurityBootstrap] UserManager 就绪，"
                + "当前注册用户数: " + userManager.getOnlineCount() + "（在线）");
    }

    /**
     * 查询安全模块是否已初始化。
     *
     * @return true 表示已调用过 {@link #init()}
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
