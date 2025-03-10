/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.converter.wire;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.ByteString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

@RunWith(TestParameterInjector.class)
public final class WireConverterFactoryTest {
  interface Service {
    @GET("/")
    Call<Phone> get();

    @POST("/")
    Call<Phone> post(@Body Phone impl);

    @POST("/")
    Call<Void> postCrashing(@Body CrashingPhone impl);

    @GET("/")
    Call<String> wrongClass();

    @GET("/")
    Call<List<String>> wrongType();
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private final Service service;
  private final boolean streaming;

  public WireConverterFactoryTest(@TestParameter boolean streaming) {
    this.streaming = streaming;

    WireConverterFactory factory = WireConverterFactory.create();
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(streaming ? factory.withStreaming() : factory)
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void serializeAndDeserialize() throws IOException, InterruptedException {
    ByteString encoded = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    Call<Phone> call = service.post(new Phone("(519) 867-5309"));
    Response<Phone> response = call.execute();
    Phone body = response.body();
    assertThat(body.number).isEqualTo("(519) 867-5309");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readByteString()).isEqualTo(encoded);
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/x-protobuf");
  }

  @Test
  public void serializeIsStreamed() throws IOException, InterruptedException {
    assumeTrue(streaming);

    Call<Void> call = service.postCrashing(new CrashingPhone("(519) 867-5309"));

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);

    // If streaming were broken, the call to enqueue would throw the exception synchronously.
    call.enqueue(
        new Callback<Void>() {
          @Override
          public void onResponse(Call<Void> call, Response<Void> response) {
            latch.countDown();
          }

          @Override
          public void onFailure(Call<Void> call, Throwable t) {
            throwableRef.set(t);
            latch.countDown();
          }
        });
    latch.await();

    Throwable throwable = throwableRef.get();
    assertThat(throwable).isInstanceOf(EOFException.class);
    assertThat(throwable).hasMessageThat().isEqualTo("oops!");
  }

  @Test
  public void deserializeEmpty() throws IOException {
    server.enqueue(new MockResponse());

    Call<Phone> call = service.get();
    Response<Phone> response = call.execute();
    Phone body = response.body();
    assertThat(body.number).isNull();
  }

  @Test
  public void deserializeWrongClass() throws IOException {
    ByteString encoded = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    try {
      service.wrongClass();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessageThat()
          .isEqualTo(
              ""
                  + "Unable to create converter for class java.lang.String\n"
                  + "    for method Service.wrongClass");
      assertThat(e.getCause())
          .hasMessageThat()
          .isEqualTo(
              ""
                  + "Could not locate ResponseBody converter for class java.lang.String.\n"
                  + "  Tried:\n"
                  + "   * retrofit2.BuiltInConverters\n"
                  + "   * retrofit2.converter.wire.WireConverterFactory\n"
                  + "   * retrofit2.OptionalConverterFactory");
    }
  }

  @Test
  public void deserializeWrongType() throws IOException {
    ByteString encoded = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    try {
      service.wrongType();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessageThat()
          .isEqualTo(
              ""
                  + "Unable to create converter for java.util.List<java.lang.String>\n"
                  + "    for method Service.wrongType");
      assertThat(e.getCause())
          .hasMessageThat()
          .isEqualTo(
              ""
                  + "Could not locate ResponseBody converter for java.util.List<java.lang.String>.\n"
                  + "  Tried:\n"
                  + "   * retrofit2.BuiltInConverters\n"
                  + "   * retrofit2.converter.wire.WireConverterFactory\n"
                  + "   * retrofit2.OptionalConverterFactory");
    }
  }

  @Test
  public void deserializeWrongValue() throws IOException {
    ByteString encoded = ByteString.decodeBase64("////");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    Call<?> call = service.get();
    try {
      call.execute();
      fail();
    } catch (EOFException ignored) {
    }
  }
}
