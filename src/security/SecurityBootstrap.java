package security;

public class SecurityBootstrap {
    private static UserManager userManager;

    public static void init() {
        userManager = UserManager.getInstance();
        System.out.println("[SecurityBootstrap] UserManager 就绪，当前注册用户数: " + userManager.getUserCount());

        if (userManager.getUserCount() == 0) {
            System.out.println("[SecurityBootstrap] 用户库为空，自动创建默认管理员账号: admin / 123456");
            AuthResult result = userManager.register("admin", "123456");
            if (result.isSuccess()) {
                System.out.println("[SecurityBootstrap] 默认管理员创建成功。");
            } else {
                System.err.println("[SecurityBootstrap] 创建默认管理员失败: " + result.getMessage());
            }
        }
    }

    // 供 ClientHandler 使用的简单认证（不改变在线状态）
    public static boolean authenticate(String username, String password) {
        if (userManager == null) {
            System.err.println("[SecurityBootstrap] 安全模块尚未初始化");
            return false;
        }
        return userManager.authenticate(username, password);
    }

    // 如果需要登录（会更新在线状态），也可以暴露 login 方法
    public static AuthResult login(String username, String password) {
        if (userManager == null) {
            return AuthResult.fail("安全模块未初始化");
        }
        return userManager.login(username, password);
    }
}