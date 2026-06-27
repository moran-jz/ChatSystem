package security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyStore;

/**
 * SSL/TLS 上下文构建器。
 *
 * <h3>用途</h3>
 * <p>
 * 为 ChatServer 提供创建 SSL ServerSocket 的能力。支持两种模式：
 * </p>
 * <ul>
 *   <li><b>生产模式</b>：从 JKS 密钥库文件加载证书和私钥。</li>
 *   <li><b>开发模式</b>：使用自签名证书（需要先生成密钥库）。</li>
 * </ul>
 *
 * <h3>在 ChatServer 中的集成方式</h3>
 * <pre>
 *   // 替代原来的 new ServerSocket(PORT)
 *   ServerSocket socket = SSLContextBuilder.getServerSocket(PORT, useSSL);
 * </pre>
 *
 * <h3>生成自签名密钥库（开发用）</h3>
 * <pre>
 *   keytool -genkeypair -alias chatssl -keyalg RSA -keysize 2048 \
 *           -storetype JKS -keystore chat.keystore -validity 365 \
 *           -storepass chatpass -keypass chatpass \
 *           -dname "CN=localhost, OU=Chat, O=ChatSystem, L=Default, S=Default, C=CN"
 * </pre>
 */
public final class SSLContextBuilder {

    /** 默认密钥库类型 */
    private static final String KEYSTORE_TYPE = "JKS";

    /** 默认 TLS 协议版本 */
    private static final String TLS_PROTOCOL = "TLSv1.2";

    /** 全局 SSL 上下文（初始化后缓存，避免重复创建） */
    private static SSLContext cachedContext;

    /** 是否启用 SSL（可通过外部配置修改） */
    private static boolean useSSL = false;

    private SSLContextBuilder() {
        // 工具类，禁止实例化
    }

    // ==================== 配置 ====================

    /**
     * 设置是否启用 SSL。
     * <p>
     * 调用方（如 ChatServer）可在启动时根据配置文件设置此标志。
     * </p>
     *
     * @param enabled true 启用 SSL
     */
    public static void setUseSSL(boolean enabled) {
        useSSL = enabled;
    }

    /**
     * 查询是否启用了 SSL。
     *
     * @return true 表示 SSL 已启用
     */
    public static boolean isUseSSL() {
        return useSSL;
    }

    // ==================== ServerSocket 工厂方法 ====================

    /**
     * 根据当前 SSL 配置返回合适的 ServerSocket。
     * <p>
     * 这是推荐给 ChatServer 使用的统一入口：
     * </p>
     * <pre>
     *   ServerSocket serverSocket = SSLContextBuilder.getServerSocket(9000);
     * </pre>
     *
     * @param port 监听端口
     * @return 如果启用 SSL 则返回 {@link SSLServerSocket}，否则返回普通 {@link ServerSocket}
     * @throws IOException 创建 Socket 失败时抛出
     */
    public static ServerSocket getServerSocket(int port) throws IOException {
        if (useSSL) {
            SSLServerSocketFactory factory = getServerSocketFactory();
            if (factory == null) {
                throw new IOException("SSL 已启用但 SSLContext 未初始化，请先调用 initSSL()");
            }
            return factory.createServerSocket(port);
        }
        return new ServerSocket(port);
    }

    /**
     * 根据参数返回合适的 ServerSocket（显式指定 useSSL 标志）。
     *
     * @param port   监听端口
     * @param useSSL 是否使用 SSL
     * @return 普通或 SSL ServerSocket
     * @throws IOException 创建 Socket 失败时抛出
     */
    public static ServerSocket getServerSocket(int port, boolean useSSL) throws IOException {
        setUseSSL(useSSL);
        return getServerSocket(port);
    }

    // ==================== SSL 初始化 ====================

    /**
     * 从 JKS 密钥库初始化 SSL 上下文。
     *
     * @param keystorePath     密钥库文件路径
     * @param keystorePassword 密钥库密码
     * @param keyPassword      密钥密码（通常与密钥库密码相同）
     * @return 初始化后的 {@link SSLContext}
     * @throws Exception 初始化失败时抛出
     */
    public static SSLContext initSSL(String keystorePath,
                                     String keystorePassword,
                                     String keyPassword) throws Exception {
        // 1. 加载密钥库
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }

        // 2. 初始化 KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyPassword.toCharArray());

        // 3. 初始化 TrustManagerFactory（信任同一密钥库中的证书）
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // 4. 创建并初始化 SSLContext
        SSLContext context = SSLContext.getInstance(TLS_PROTOCOL);
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        // 5. 缓存
        cachedContext = context;
        useSSL = true;

        System.out.println("[SSLContextBuilder] SSL 上下文初始化成功: " + keystorePath);
        return context;
    }

    /**
     * 获取缓存的 SSLServerSocketFactory。
     *
     * @return SSLServerSocketFactory，若未初始化则返回 null
     */
    public static SSLServerSocketFactory getServerSocketFactory() {
        if (cachedContext == null) {
            return null;
        }
        return cachedContext.getServerSocketFactory();
    }

    /**
     * 重置 SSL 配置（用于测试或热切换）。
     */
    public static void reset() {
        cachedContext = null;
        useSSL = false;
    }
}
