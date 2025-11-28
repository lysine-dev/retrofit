package retrofit2;

import okhttp3.*;
import java.util.Map;

final class RequestFactory {

  enum RequestType { GET, POST, MULTIPART, FORM }

  private final RequestType requestType;
  private final String baseUrl;

  RequestFactory(RequestType type, String url) {
    Utils.validateInputs(url, type.name());
    this.requestType = type;
    this.baseUrl = Utils.normalizeUrl(url);
  }

  Request create(Map<String, String> headers, Map<String, String> queryParams, RequestBody body) {
    String finalUrl = Utils.buildFinalUrl(baseUrl, queryParams);
    switch(requestType) {
      case MULTIPART: return createMultipartRequest(finalUrl, headers, body);
      case FORM: return createFormRequest(finalUrl, headers, body);
      case GET: return createGetRequest(finalUrl, headers);
      case POST: return createPostRequest(finalUrl, headers, body);
      default: throw new IllegalStateException("Unknown request type: " + requestType);
    }
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
