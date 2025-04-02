/*
 * Copyright (C) 2025 Square, Inc.
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

import static retrofit2.converter.gson.GsonRequestBodyConverter.MEDIA_TYPE;
import static retrofit2.converter.gson.GsonRequestBodyConverter.writeJson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

final class GsonStreamingRequestBody<T> extends RequestBody {
  private final Gson gson;
  private final TypeAdapter<T> adapter;
  private final T value;

  public GsonStreamingRequestBody(Gson gson, TypeAdapter<T> adapter, T value) {
    this.gson = gson;
    this.adapter = adapter;
    this.value = value;
  }

  @Override
  public MediaType contentType() {
    return MEDIA_TYPE;
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    writeJson(sink, gson, adapter, value);
  }
}
