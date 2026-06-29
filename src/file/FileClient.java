package file;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import log.Logger;
/**
 * 文件客户端，通过 HTTP 与 FileHttpServer 交互实现上传和下载。
 */
public class FileClient {

    /**
     * 上传文件到服务器
     * @param filePath 本地文件路径
     * @param serverBaseUrl 服务器基础 URL，例如 http://localhost:8080
     * @param remoteFileName 服务器端保存的文件名（可自行命名）
     * @return 是否成功
     */
    public boolean uploadFile(String filePath, String serverBaseUrl, String remoteFileName) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                Logger.getInstance().error("File not found: " + filePath);
                return false;
            }

            String urlStr = serverBaseUrl + "/upload?filename=" + URLEncoder.encode(remoteFileName, "UTF-8");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = conn.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Logger.getInstance().info("Upload successful: " + remoteFileName);
                return true;
            } else {
                Logger.getInstance().error("Upload failed, HTTP code: " + responseCode);
                return false;
            }
        } catch (IOException e) {
            Logger.getInstance().error("Upload error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从服务器下载文件
     * @param serverBaseUrl 服务器基础 URL
     * @param remoteFileName 服务器上的文件名
     * @param saveDir 本地保存目录
     * @return 是否成功
     */
    public boolean downloadFile(String serverBaseUrl, String remoteFileName, String saveDir) {
        try {
            String urlStr = serverBaseUrl + "/download/" + URLEncoder.encode(remoteFileName, "UTF-8");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                Logger.getInstance().error("Download failed, HTTP code: " + responseCode);
                return false;
            }

            // 创建保存目录
            File dir = new File(saveDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String safeFileName = new File(remoteFileName).getName();
            File saveFile = new File(dir, safeFileName);
            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(saveFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            Logger.getInstance().info("Download successful: " + remoteFileName + " saved to " + saveDir);
            return true;
        } catch (IOException e) {
            Logger.getInstance().error("Download error: " + e.getMessage());
            return false;
        }
    }

    // 测试示例
    public static void main(String[] args) {
        FileClient client = new FileClient();
        // 上传
        client.uploadFile("test.txt", "http://localhost:8080", "mytest.txt");
        // 下载
        client.downloadFile("http://localhost:8080", "mytest.txt", "./downloads/");
    }
}
