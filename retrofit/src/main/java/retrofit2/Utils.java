package retrofit2;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.HttpUrl;
import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

final class Utils {

  private static final Logger logger = Logger.getLogger(Utils.class.getName());

  private Utils() {
    throw new AssertionError("No instances.");
  }

  static void validateInputs(String url, String method) {
    if (url == null || url.isEmpty()) throw new IllegalArgumentException("URL must not be null or empty.");
    if (method == null || method.isEmpty()) throw new IllegalArgumentException("HTTP method must not be null or empty.");
  }

  static File resolveFile(String relativePath) {
    String projectRoot = System.getProperty("user.dir");
    File f = new File(projectRoot, relativePath);
    if (!f.exists()) logger.warning("File does not exist: " + f.getAbsolutePath());
    return f;
  }

  static RequestBody createBody(File file) {
    return RequestBody.create(MediaType.parse(determineMimeType(file)), file);
  }

  // New helper to remove duplication
  static String determineMimeType(File file) {
    String name = file.getName();
    int dot = name.lastIndexOf('.');
    if (dot == -1) return "application/octet-stream";
    String ext = name.substring(dot + 1).toLowerCase();
    switch (ext) {
      case "txt": return "text/plain";
      case "json": return "application/json";
      case "xml": return "application/xml";
      default: return "application/octet-stream";
    }
  }

  static String normalizeUrl(String url) {
    HttpUrl parsed = HttpUrl.parse(url);
    if (parsed == null) throw new IllegalArgumentException("Malformed URL: " + url);
    return parsed.toString();
  }

  static String buildFinalUrl(String baseUrl, Map<String, String> queryParams) {
    HttpUrl.Builder builder = HttpUrl.parse(baseUrl).newBuilder();
    for (Map.Entry<String, String> e : queryParams.entrySet()) builder.addQueryParameter(e.getKey(), e.getValue());
    return builder.build().toString();
  }
}
