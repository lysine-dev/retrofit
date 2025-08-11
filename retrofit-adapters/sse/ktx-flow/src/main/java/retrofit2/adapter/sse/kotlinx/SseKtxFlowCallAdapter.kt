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
package retrofit2.adapter.sse.kotlinx

import java.lang.reflect.Type
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.sse.ServerSentEvent
import retrofit2.awaitResponse

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : Any> conversionError(value: T, type: Type): Nothing =
  error("Failed to convert $value to $type, actual type is ${value.javaClass}")

internal class SseKtxFlowCallAdapter<ID : Any, TYPE : Any, DATA : Any>(
  private val idType: Type,
  private val typeType: Type,
  private val dataType: Type,
  private val idConverter: Converter<ResponseBody, ID?>,
  private val typeConverter: Converter<ResponseBody, TYPE?>,
  private val dataConverter: Converter<ResponseBody, DATA>,
  retrofit: Retrofit,
) : CallAdapter<ResponseBody, Flow<ServerSentEvent<ID, TYPE, DATA>>> {

  override fun responseType(): Type = ResponseBody::class.java

  override fun adapt(
    call: Call<ResponseBody>,
  ): Flow<ServerSentEvent<ID, TYPE, DATA>> = callbackFlow {
    val response = call.awaitResponse()
    val okhttpResponse = response.raw().newBuilder().body(response.body() ?: error("Response body is null")).build()

    EventSources.processResponse(
      okhttpResponse,
      object : EventSourceListener() {
        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
          val convertedId = convertId(id)
          val convertedType = convertType(type)
          val convertedData = convertData(data)
          trySendBlocking(ServerSentEvent(convertedId, convertedType, convertedData))
        }

        override fun onClosed(eventSource: EventSource) {
          close()
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
          close(t ?: RuntimeException()) // TODO
        }
      },
    )

    awaitClose(call::cancel)
  }

  private fun convertId(id: String?): ID? {
    return if (id != null) idConverter.convert(id.toResponseBody()) ?: conversionError(id, idType) else null
  }

  private fun convertType(type: String?): TYPE? {
    return if (type != null) typeConverter.convert(type.toResponseBody()) ?: conversionError(type, typeType) else null
  }

  private fun convertData(data: String): DATA {
    return dataConverter.convert(data.toResponseBody()) ?: conversionError(data, dataType)
  }

}
