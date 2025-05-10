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
package retrofit2.converter.protobuf;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static retrofit2.converter.protobuf.PhoneProtos.Phone;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
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
public final class ProtoConverterFactoryTest {
  interface Service {
    @GET("/")
    Call<Phone> get();

    @POST("/")
    Call<Phone> post(@Body MessageLite impl);

    @GET("/")
    Call<String> wrongClass();

    @GET("/")
    Call<List<String>> wrongType();
  }

  interface ServiceWithRegistry {
    @GET("/")
    Call<Phone> get();
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private final Service service;
  private final ServiceWithRegistry serviceWithRegistry;
  private final boolean streaming;

  public ProtoConverterFactoryTest(@TestParameter boolean streaming) {
    this.streaming = streaming;

    ExtensionRegistry registry = ExtensionRegistry.newInstance();
    PhoneProtos.registerAllExtensions(registry);

    ProtoConverterFactory factory = ProtoConverterFactory.create();
    ProtoConverterFactory factoryWithRegistry = ProtoConverterFactory.createWithRegistry(registry);
    if (streaming) {
      factory = factory.withStreaming();
      factoryWithRegistry = factoryWithRegistry.withStreaming();
    }

    Retrofit retrofit =
        new Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(factory).build();
    service = retrofit.create(Service.class);

    Retrofit retrofitWithRegistry =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(factoryWithRegistry)
            .build();
    serviceWithRegistry = retrofitWithRegistry.create(ServiceWithRegistry.class);
  }

  @Test
  public void serializeAndDeserialize() throws IOException, InterruptedException {
    ByteString encoded = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    Call<Phone> call = service.post(Phone.newBuilder().setNumber("(519) 867-5309").build());
    Response<Phone> response = call.execute();
    Phone body = response.body();
    assertThat(body.getNumber()).isEqualTo("(519) 867-5309");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readByteString()).isEqualTo(encoded);
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/x-protobuf");
  }

  @Test
  public void deserializeEmpty() throws IOException {
    server.enqueue(new MockResponse());

    Call<Phone> call = service.get();
    Response<Phone> response = call.execute();
    Phone body = response.body();
    assertThat(body.hasNumber()).isFalse();
  }

  @Test
  public void deserializeUsesRegistry() throws IOException {
    ByteString encoded = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwORAB");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    Call<Phone> call = serviceWithRegistry.get();
    Response<Phone> response = call.execute();
    Phone body = response.body();
    assertThat(body.getNumber()).isEqualTo("(519) 867-5309");
    assertThat(body.getExtension(PhoneProtos.voicemail)).isEqualTo(true);
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
                  + "   * retrofit2.converter.protobuf.ProtoConverterFactory\n"
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
                  + "   * retrofit2.converter.protobuf.ProtoConverterFactory\n"
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
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      assertThat(cause).isInstanceOf(InvalidProtocolBufferException.class);
      assertThat(cause).hasMessageThat().contains("input ended unexpectedly");
    }
  }

  @Test
  public void serializeIsStreamed() throws InterruptedException {
    assumeTrue(streaming);

    Call<Phone> call =
        service.post(new ThrowingPhone(Phone.newBuilder().setNumber("(519) 867-5309").build()));

    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);

    // If streaming were broken, the call to enqueue would throw the exception synchronously.
    call.enqueue(
        new Callback<Phone>() {
          @Override
          public void onResponse(Call<Phone> call, Response<Phone> response) {
            latch.countDown();
          }

          @Override
          public void onFailure(Call<Phone> call, Throwable t) {
            throwableRef.set(t);
            latch.countDown();
          }
        });
    latch.await();

    Throwable throwable = throwableRef.get();
    assertThat(throwable).isInstanceOf(EOFException.class);
    assertThat(throwable).hasMessageThat().isEqualTo("oops!");
  }

  private static final class ThrowingPhone extends AbstractMessageLite {
    private final Phone delegate;

    private ThrowingPhone(Phone delegate) {
      this.delegate = delegate;
    }

    @Override
    public void writeTo(CodedOutputStream output) throws IOException {
      throw new EOFException("oops!");
    }

    @Override
    public int getSerializedSize() {
      return delegate.getSerializedSize();
    }

    @Override
    public Parser<? extends MessageLite> getParserForType() {
      return delegate.getParserForType();
    }

    @Override
    public MessageLite.Builder newBuilderForType() {
      return delegate.newBuilderForType();
    }

    @Override
    public MessageLite.Builder toBuilder() {
      return delegate.toBuilder();
    }

    @Override
    public MessageLite getDefaultInstanceForType() {
      return delegate.getDefaultInstanceForType();
    }

    @Override
    public boolean isInitialized() {
      return delegate.isInitialized();
    }
  }
}
