package security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 密码工具类 — SHA-256 + 随机盐（salt）。
 *
 * <h3>设计说明</h3>
 * <ol>
 *   <li><b>盐（salt）</b>：使用 {@link SecureRandom} 生成 16 字节随机盐，
 *       以十六进制字符串表示。</li>
 *   <li><b>哈希</b>：对 {@code password + salt} 拼接后的字节序列做 SHA-256 摘要，
 *       输出为 64 字符的十六进制字符串。</li>
 *   <li><b>存储格式</b>："{@code salt:hash}"，通过冒号分隔，便于持久化和解析。</li>
 * </ol>
 *
 * <pre>
 * 使用示例：
 *   String stored = PasswordUtil.hash("mypassword");          // → "a1b2...:c3d4..."
 *   boolean ok = PasswordUtil.verify("mypassword", stored);   // → true
 * </pre>
 */
public final class PasswordUtil {

    /** 盐的字节长度 */
    private static final int SALT_LENGTH = 16;

    /** 哈希算法 */
    private static final String ALGORITHM = "SHA-256";

    /** 安全随机数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
        // 工具类，禁止实例化
    }

    // ==================== 公开方法 ====================

    /**
     * 对明文密码执行加盐哈希，返回 "{@code salt:hash}" 格式的存储值。
     *
     * @param plainPassword 明文密码，不能为 null 或空
     * @return 盐与哈希拼接字符串，格式 {@code salt:hash}
     * @throws IllegalArgumentException 如果密码为 null 或空
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 1. 生成随机盐
        String salt = generateSalt();

        // 2. 计算 salt + password 的 SHA-256
        String hash = sha256(salt + plainPassword);

        // 3. 返回 salt:hash
        return salt + ":" + hash;
    }

    /**
     * 验证明文密码是否与 {@code salt:hash} 存储值匹配。
     *
     * @param plainPassword 明文密码
     * @param storedValue   之前通过 {@link #hash(String)} 生成的存储值
     * @return true 表示密码匹配
     */
    public static boolean verify(String plainPassword, String storedValue) {
        if (plainPassword == null || storedValue == null) {
            return false;
        }

        // 1. 解析 salt:hash
        int colonIndex = storedValue.indexOf(':');
        if (colonIndex <= 0 || colonIndex == storedValue.length() - 1) {
            return false; // 格式不合法
        }

        String salt = storedValue.substring(0, colonIndex);
        String expectedHash = storedValue.substring(colonIndex + 1);

        // 2. 用相同的盐重新计算哈希
        String computedHash = sha256(salt + plainPassword);

        // 3. 常量时间比对（防止时序攻击）
        return constantTimeEquals(expectedHash, computedHash);
    }

    // ==================== 内部方法 ====================

    /**
     * 生成一个随机的十六进制盐字符串。
     *
     * @return 32 字符的十六进制盐（源于 16 字节随机数据）
     */
    static String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(saltBytes);
        return HexFormat.of().formatHex(saltBytes);
    }

    /**
     * 对输入字符串计算 SHA-256 摘要，返回十六进制字符串。
     *
     * @param input 输入字符串
     * @return 64 字符的十六进制哈希值
     */
    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Java 规范要求 SHA-256 必须存在，此异常理论不会发生
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 常量时间字符串比较，防止时序攻击泄露哈希信息。
     * <p>
     * 无论两个字符串在哪个位置不同，比较耗时均相等。
     * </p>
     *
     * @param a 字符串 a
     * @param b 字符串 b
     * @return true 表示两个字符串相等
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] ba = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (ba.length != bb.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < ba.length; i++) {
            diff |= ba[i] ^ bb[i];
        }
        return diff == 0;
    }
}
