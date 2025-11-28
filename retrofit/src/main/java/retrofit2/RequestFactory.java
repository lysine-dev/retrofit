package retrofit2;

import okhttp3.*;
import java.util.Map;
import java.util.logging.Logger;

final class RequestFactory {

  private static final Logger logger = Logger.getLogger(RequestFactory.class.getName());

  private final String httpMethod;
  private final String baseUrl;

  RequestFactory(String method, String url) {
    try {
      Utils.validateInputs(url, method);
    } catch (Exception e) {
      logger.severe("RequestFactory initialization failed: " + e.getMessage());
      throw e;
    }
    this.httpMethod = method;
    this.baseUrl = Utils.normalizeUrl(url);
  }

  Request create(Map<String, String> headers, Map<String, String> queryParams, RequestBody body) {
    String finalUrl = Utils.buildFinalUrl(baseUrl, queryParams);
    Request.Builder builder = new Request.Builder().url(finalUrl);
    if ("GET".equalsIgnoreCase(httpMethod)) builder.get();
    else builder.method(httpMethod, body);
    for (Map.Entry<String, String> h : headers.entrySet()) builder.addHeader(h.getKey(), h.getValue());
    return builder.build();
  }
}
