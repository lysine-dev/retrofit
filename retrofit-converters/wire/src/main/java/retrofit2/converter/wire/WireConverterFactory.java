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

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * A {@linkplain Converter.Factory converter} that uses Wire for protocol buffers.
 *
 * <p>This converter only applies for types which extend from {@link Message}.
 */
public final class WireConverterFactory extends Converter.Factory {
  /**
   * Create an instance which serializes request messages to bytes eagerly on the caller thread
   * when either {@link Call#execute()} or {@link Call#enqueue} is called. Response bytes are
   * always converted to message instances on one of OkHttp's background threads.
   */
  public static WireConverterFactory create() {
    return new WireConverterFactory(false);
  }

  private final boolean streaming;

  private WireConverterFactory(boolean streaming) {
    this.streaming = streaming;
  }

  /**
   * Return a new factory which streams serialization of request messages to bytes on the HTTP thread
   * This is either the calling thread for {@link Call#execute()}, or one of OkHttp's background
   * threads for {@link Call#enqueue}. Response bytes are always converted to message instances on
   * one of OkHttp's background threads.
   */
  public WireConverterFactory withStreaming() {
    return new WireConverterFactory(true);
  }

  @Override
  public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    if (!(type instanceof Class<?>)) {
      return null;
    }
    Class<?> c = (Class<?>) type;
    if (!Message.class.isAssignableFrom(c)) {
      return null;
    }
    //noinspection unchecked
    ProtoAdapter<? extends Message> adapter = ProtoAdapter.get((Class<? extends Message>) c);
    return new WireResponseBodyConverter<>(adapter);
  }

  @Override
  public @Nullable Converter<?, RequestBody> requestBodyConverter(
      Type type,
      Annotation[] parameterAnnotations,
      Annotation[] methodAnnotations,
      Retrofit retrofit) {
    if (!(type instanceof Class<?>)) {
      return null;
    }
    Class<?> c = (Class<?>) type;
    if (!Message.class.isAssignableFrom(c)) {
      return null;
    }
    //noinspection unchecked
    ProtoAdapter<? extends Message> adapter = ProtoAdapter.get((Class<? extends Message>) c);
    return new WireRequestBodyConverter<>(adapter, streaming);
  }
}
