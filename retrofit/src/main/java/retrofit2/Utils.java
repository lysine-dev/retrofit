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
    try {
      if (url == null || url.isEmpty()) throw new IllegalArgumentException("URL must not be null or empty.");
      if (method == null || method.isEmpty()) throw new IllegalArgumentException("HTTP method must not be null or empty.");
    } catch (Exception e) {
      logger.severe("Validation failed: " + e.getMessage());
      throw e;
    }
  }

  static File resolveFile(String relativePath) {
    String projectRoot = System.getProperty("user.dir");
    File f = new File(projectRoot, relativePath);
    if (!f.exists()) logger.warning("File does not exist: " + f.getAbsolutePath());
    return f;
  }

  static RequestBody createBody(File file) {
    try {
      return RequestBody.create(MediaType.parse("application/octet-stream"), file);
    } catch (Exception e) {
      logger.severe("Failed to create RequestBody: " + e.getMessage());
      throw e;
    }
  }

  static String normalizeUrl(String url) {
    HttpUrl parsed = HttpUrl.parse(url);
    if (parsed == null) {
      logger.severe("Malformed URL: " + url);
      throw new IllegalArgumentException("Malformed URL: " + url);
    }
    return parsed.toString();
  }

  static String buildFinalUrl(String baseUrl, Map<String, String> queryParams) {
    HttpUrl.Builder builder = HttpUrl.parse(baseUrl).newBuilder();
    for (Map.Entry<String, String> e : queryParams.entrySet()) builder.addQueryParameter(e.getKey(), e.getValue());
    return builder.build().toString();
  }
}
