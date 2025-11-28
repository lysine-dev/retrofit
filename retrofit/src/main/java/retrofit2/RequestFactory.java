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

  Request create(Map<String, String> headers, Map<String, String> queryParams, RequestBody body, boolean isMultipart, boolean isForm) {
    String finalUrl = Utils.buildFinalUrl(baseUrl, queryParams);

    // Replaced long if-else with "strategy-style" conditional
    if (isMultipart) return createMultipartRequest(finalUrl, headers, body);
    if (isForm) return createFormRequest(finalUrl, headers, body);
    if ("GET".equalsIgnoreCase(httpMethod)) return createGetRequest(finalUrl, headers);
    return createPostRequest(finalUrl, headers, body);
  }

  private Request createGetRequest(String url, Map<String, String> headers) {
    Request.Builder builder = new Request.Builder().url(url).get();
    attachHeaders(builder, headers);
    return builder.build();
  }

  private Request createPostRequest(String url, Map<String, String> headers, RequestBody body) {
    Request.Builder builder = new Request.Builder().url(url).method("POST", body);
    attachHeaders(builder, headers);
    return builder.build();
  }

  private Request createMultipartRequest(String url, Map<String, String> headers, RequestBody body) {
    MultipartBody multipart = new MultipartBody.Builder().setType(MultipartBody.FORM).addPart(body).build();
    Request.Builder builder = new Request.Builder().url(url).post(multipart);
    attachHeaders(builder, headers);
    return builder.build();
  }

  private Request createFormRequest(String url, Map<String, String> headers, RequestBody body) {
    Request.Builder builder = new Request.Builder().url(url).post(body);
    attachHeaders(builder, headers);
    return builder.build();
  }

  private void attachHeaders(Request.Builder builder, Map<String, String> headers) {
    for (Map.Entry<String, String> h : headers.entrySet()) builder.addHeader(h.getKey(), h.getValue());
  }
}
