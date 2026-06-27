package log;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 简单日志记录器（单例），支持 INFO、WARNING、ERROR、DEBUG 级别。
 * 日志文件保存在 logs/ 目录下，按日期滚动。
 */
public class Logger {
    private static Logger instance;
    private final String logDir = "logs/";
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private Logger() {
        File dir = new File(logDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String logLine = String.format("[%s] [%s] %s", timestamp, level, message);
        // 控制台输出
        System.out.println(logLine);
        // 写入文件
        String fileName = logDir + "app-" + LocalDateTime.now().format(dateFormatter) + ".log";
        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(logLine);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    public void info(String msg) {
        log("INFO", msg);
    }

    public void warning(String msg) {
        log("WARNING", msg);
    }

    public void error(String msg) {
        log("ERROR", msg);
    }

    public void debug(String msg) {
        log("DEBUG", msg);
    }
}