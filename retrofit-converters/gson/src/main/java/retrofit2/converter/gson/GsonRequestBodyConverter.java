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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import retrofit2.Converter;

final class GsonRequestBodyConverter<T> implements Converter<T, RequestBody> {
  static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");

  private final Gson gson;
  private final TypeAdapter<T> adapter;
  private final boolean streaming;

  GsonRequestBodyConverter(Gson gson, TypeAdapter<T> adapter, boolean streaming) {
    this.gson = gson;
    this.adapter = adapter;
    this.streaming = streaming;
  }

  @Override
  public RequestBody convert(T value) throws IOException {
    if (streaming) {
      return new GsonStreamingRequestBody<>(gson, adapter, value);
    }

    Buffer buffer = new Buffer();
    writeJson(buffer, gson, adapter, value);
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }

  static <T> void writeJson(BufferedSink sink, Gson gson, TypeAdapter<T> adapter, T value)
      throws IOException {
    Writer writer = new OutputStreamWriter(sink.outputStream(), UTF_8);
    JsonWriter jsonWriter = gson.newJsonWriter(writer);
    adapter.write(jsonWriter, value);
    jsonWriter.close();
  }
}
