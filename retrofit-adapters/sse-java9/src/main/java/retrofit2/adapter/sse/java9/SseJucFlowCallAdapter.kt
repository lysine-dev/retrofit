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
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Flow
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.SubmissionPublisher
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

internal class SseJucFlowCallAdapter<ID : Any, TYPE : Any, DATA : Any>(
  private val idType: Type,
  private val typeType: Type,
  private val dataType: Type,
  private val idConverter: Converter<String?, ID?>,
  private val typeConverter: Converter<String?, TYPE?>,
  private val dataConverter: Converter<String, DATA>,
  private val retrofit: Retrofit,
) : CallAdapter<Any, Flow.Publisher<ServerSentEvent<ID, TYPE, DATA>>> {

  private val executor: Executor =
    retrofit.callbackExecutor()
      ?: ForkJoinPool.commonPool().takeIf { ForkJoinPool.getCommonPoolParallelism() > 1 }
      ?: Executors.newCachedThreadPool()

  private val responseType = object : ParameterizedType {
    override fun getActualTypeArguments(): Array<Type> {
      return arrayOf(idType, typeType, dataType)
    }

    override fun getOwnerType(): Type? {
      return null
    }

    override fun getRawType(): Type {
      return Flow.Publisher::class.java
    }
  }

  override fun responseType(): Type = responseType

  override fun adapt(call: Call<in Any>): Flow.Publisher<ServerSentEvent<ID, TYPE, DATA>> = SsePublisher(call)

  inner class SsePublisher(
    private val call: Call<Any>,
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

  inner class PublisherCallback(private val publisher: SubmissionPublisher<ServerSentEvent<ID, TYPE, DATA>>) :
    Callback<Any> {
    override fun onResponse(call: Call<Any>, response: Response<Any>) {
      EventSources.processResponse(
        response.raw(),
        object : EventSourceListener() {
          override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            val convertedId =
              if (id != null) idConverter.convert(id) ?: error("Failed to convert $id to $idType") else null
            val convertedType =
              if (type != null) typeConverter.convert(type) ?: error("Failed to convert $type to $typeType") else null
            val convertedData = dataConverter.convert(data) ?: error("Failed to convert $data to $dataType")
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

    override fun onFailure(call: Call<Any>, t: Throwable) {
      publisher.closeExceptionally(t)
    }
  }

}
