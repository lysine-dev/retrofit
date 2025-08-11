/*
 * Copyright (C) 2017 Square, Inc.
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
package retrofit2.adapter.sse.java9

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.Flow
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.sse.ServerSentEvent
import retrofit2.http.Streaming

private val EMPTY_ARRAY = emptyArray<Annotation>()

object SseJucFlowCallAdapterFactory : CallAdapter.Factory() {

  override fun get(
    returnType: Type,
    annotations: Array<out Annotation?>,
    retrofit: Retrofit,
  ): CallAdapter<*, *>? {
    if (getRawType(returnType) != Flow.Publisher::class.java) {
      return null
    }

    if (returnType !is ParameterizedType) {
      error(
        "Flow.Publisher return type must be parameterized as Flow.Publisher<Foo> or Flow.Publisher<? extends Foo>",
      )
    }

    val innerType = getParameterUpperBound(0, returnType)

    if (getRawType(innerType) != ServerSentEvent::class.java) {
      return null
    }

    if (innerType !is ParameterizedType) {
      error(
        "ServerSentEvent must be parameterized as ServerSentEvent<ID, TYPE, DATA>" +
          " or ServerSentEvent<? extends ID, ? extends TYPE, ? extends DATA>",
      )
    }

    if (annotations.none { it is Streaming }) {
      error("SSE endpoint must be annotated with @Streaming")
    }

    val idType = getParameterUpperBound(0, innerType)
    val typeType = getParameterUpperBound(1, innerType)
    val dataType = getParameterUpperBound(2, innerType)

    return SseJucFlowCallAdapter(
      idType,
      typeType,
      dataType,
      retrofit.stringConverter(idType, EMPTY_ARRAY),
      retrofit.stringConverter(typeType, EMPTY_ARRAY),
      retrofit.stringConverter(dataType, EMPTY_ARRAY),
      retrofit,
    )
  }
}
