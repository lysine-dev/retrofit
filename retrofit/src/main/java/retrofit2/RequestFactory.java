package retrofit2;

import okhttp3.*;
import java.util.Map;

final class RequestFactory {

  private final String httpMethod;
  private final String baseUrl;

  RequestFactory(String method, String url) {
    Utils.validateInputs(url, method);
    this.httpMethod = method;
    this.baseUrl = Utils.normalizeUrl(url);
  }

  Request create(Map<String, String> headers, Map<String, String> queryParams, RequestBody body) {
    String finalUrl = Utils.buildFinalUrl(baseUrl, queryParams);
    return buildRequest(finalUrl, headers, body);
  }

  // Reusable helper to remove duplicate header logic
  private Request buildRequest(String url, Map<String, String> headers, RequestBody body) {
    Request.Builder builder = new Request.Builder().url(url);
    if ("GET".equalsIgnoreCase(httpMethod)) builder.get();
    else builder.method(httpMethod, body);

    attachHeaders(builder, headers);
    return builder.build();
  }

  private void attachHeaders(Request.Builder builder, Map<String, String> headers) {
    for (Map.Entry<String, String> h : headers.entrySet()) builder.addHeader(h.getKey(), h.getValue());
  }
}
