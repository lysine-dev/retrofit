/*
 * Copyright (C) 2015 Square, Inc.
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
package retrofit2.converter.gson;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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
public final class GsonConverterFactoryTest {
  interface AnInterface {
    String getName();
  }

  static class AnImplementation implements AnInterface {
    private final String theName;

    AnImplementation(String name) {
      theName = name;
    }

    @Override
    public String getName() {
      return theName;
    }
  }

  static final class ErroringValue {
    static final TypeAdapter<ErroringValue> BROKEN_ADAPTER =
        new TypeAdapter<ErroringValue>() {
          @Override
          public void write(JsonWriter out, ErroringValue value) throws IOException {
            throw new EOFException("oops!");
          }

          @Override
          @SuppressWarnings("CheckReturnValue")
          public ErroringValue read(JsonReader reader) throws IOException {
            reader.beginObject();
            reader.nextName();
            String theName = reader.nextString();
            return new ErroringValue(theName);
          }
        };

    final String theName;

    ErroringValue(String theName) {
      this.theName = theName;
    }
  }

  static class AnInterfaceAdapter extends TypeAdapter<AnInterface> {
    @Override
    public void write(JsonWriter jsonWriter, AnInterface anInterface) throws IOException {
      jsonWriter.beginObject();
      jsonWriter.name("name").value(anInterface.getName());
      jsonWriter.endObject();
    }

    @Override
    public AnInterface read(JsonReader jsonReader) throws IOException {
      jsonReader.beginObject();

      String name = null;
      while (jsonReader.peek() != JsonToken.END_OBJECT) {
        switch (jsonReader.nextName()) {
          case "name":
            name = jsonReader.nextString();
            break;
        }
      }

      jsonReader.endObject();
      return new AnImplementation(name);
    }
  }

  interface Service {
    @POST("/")
    Call<AnImplementation> anImplementation(@Body AnImplementation impl);

    @POST("/")
    Call<AnInterface> anInterface(@Body AnInterface impl);

    @GET("/")
    Call<ErroringValue> readErroringValue();

    @POST("/")
    Call<Void> writeErroringValue(@Body ErroringValue value);
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private final boolean streaming;
  private final Service service;

  public GsonConverterFactoryTest(@TestParameter boolean streaming) {
    this.streaming = streaming;

    Gson gson =
        new GsonBuilder()
            .registerTypeAdapter(AnInterface.class, new AnInterfaceAdapter())
            .registerTypeAdapter(ErroringValue.class, ErroringValue.BROKEN_ADAPTER)
            .setLenient()
            .create();

    GsonConverterFactory factory = GsonConverterFactory.create(gson);
    if (streaming) {
      factory = factory.withStreaming();
    }

    Retrofit retrofit =
        new Retrofit.Builder() //
            .baseUrl(server.url("/")) //
            .addConverterFactory(factory) //
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void anInterface() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));

    Call<AnInterface> call = service.anInterface(new AnImplementation("value"));
    Response<AnInterface> response = call.execute();
    AnInterface body = response.body();
    assertThat(body.getName()).isEqualTo("value");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test
  public void anImplementation() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));

    Call<AnImplementation> call = service.anImplementation(new AnImplementation("value"));
    Response<AnImplementation> response = call.execute();
    AnImplementation body = response.body();
    assertThat(body.theName).isEqualTo("value");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"theName\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test
  public void serializeUsesConfiguration() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{}"));

    service.anImplementation(new AnImplementation(null)).execute();

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{}"); // Null value was not serialized.
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test
  public void deserializeUsesConfiguration() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{/* a comment! */}"));

    Response<AnImplementation> response =
        service.anImplementation(new AnImplementation("value")).execute();
    assertThat(response.body().getName()).isNull();
  }

  @Test
  public void requireFullResponseDocumentConsumption() throws Exception {
    server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));

    Call<ErroringValue> call = service.readErroringValue();
    try {
      call.execute();
      fail();
    } catch (JsonIOException e) {
      assertThat(e).hasMessageThat().isEqualTo("JSON document was not fully consumed.");
    }
  }

  @Test
  public void serializeIsStreamed() throws InterruptedException {
    assumeTrue(streaming);

    Call<Void> call = service.writeErroringValue(new ErroringValue("hi"));

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
}
