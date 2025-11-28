package retrofit2;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.HttpUrl;
import java.io.File;
import java.util.Map;

final class Utils {

  private Utils() {
    throw new AssertionError("No instances.");
  }

  static void validateInputs(String url, String method) {
    if (url == null || url.isEmpty()) throw new IllegalArgumentException("URL must not be null or empty.");
    if (method == null || method.isEmpty()) throw new IllegalArgumentException("HTTP method must not be null or empty.");
  }

  // Hardcoded paths replaced with dynamic resolution
  static File resolveFile(String relativePath) {
    String projectRoot = System.getProperty("user.dir");
    return new File(projectRoot, relativePath);
  }

  static RequestBody createBody(File file) {
    return RequestBody.create(MediaType.parse("application/octet-stream"), file);
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
