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

import java.lang.reflect.Type
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Flow
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.SubmissionPublisher
import okhttp3.ResponseBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.sse.ServerSentEvent

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : Any> conversionError(value: T, type: Type): Nothing =
  error("Failed to convert $value to $type, actual type is ${value.javaClass}")

internal class SseJucFlowCallAdapter<ID : Any, TYPE : Any, DATA : Any>(
  private val idType: Type,
  private val typeType: Type,
  private val dataType: Type,
  private val idConverter: Converter<String?, ID?>,
  private val typeConverter: Converter<String?, TYPE?>,
  private val dataConverter: Converter<String, DATA>,
  retrofit: Retrofit,
) : CallAdapter<ResponseBody, Flow.Publisher<ServerSentEvent<ID, TYPE, DATA>>> {

  private val executor: Executor =
    retrofit.callbackExecutor()
      ?: ForkJoinPool.commonPool().takeIf { ForkJoinPool.getCommonPoolParallelism() > 1 }
      ?: Executors.newCachedThreadPool()

  override fun responseType(): Type = ResponseBody::class.java

  override fun adapt(
    call: Call<ResponseBody>,
  ): Flow.Publisher<ServerSentEvent<ID, TYPE, DATA>> = SsePublisher(call)

  inner class SsePublisher(
    private val call: Call<ResponseBody>,
  ) : SubmissionPublisher<ServerSentEvent<ID, TYPE, DATA>>(executor, Flow.defaultBufferSize()) {
    override fun subscribe(subscriber: Flow.Subscriber<in ServerSentEvent<ID, TYPE, DATA>>?) {
      super.subscribe(subscriber)
      call.enqueue(PublisherCallback(this))
    }

    override fun close() {
      call.cancel()
      super.close()
    }
  }

  inner class PublisherCallback(
    private val publisher: SubmissionPublisher<ServerSentEvent<ID, TYPE, DATA>>,
  ) : Callback<ResponseBody> {
    override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
      EventSources.processResponse(
        response.raw().newBuilder().body(response.body() ?: error("Response body is null")).build(),
        object : EventSourceListener() {
          override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            val convertedId = convertId(id)
            val convertedType = convertType(type)
            val convertedData = convertData(data)
            publisher.submit(ServerSentEvent(convertedId, convertedType, convertedData))
          }

          override fun onClosed(eventSource: EventSource) {
            publisher.close()
          }

          override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
            publisher.closeExceptionally(t ?: RuntimeException()) // TODO
          }
        },
      )
    }

    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
      publisher.closeExceptionally(t)
    }
  }

  private fun convertId(id: String?): ID? {
    return if (id != null) idConverter.convert(id) ?: conversionError(id, idType) else null
  }

  private fun convertType(type: String?): TYPE? {
    return if (type != null) typeConverter.convert(type) ?: conversionError(type, typeType) else null
  }

  private fun convertData(data: String): DATA {
    return dataConverter.convert(data) ?: conversionError(data, dataType)
  }

}
