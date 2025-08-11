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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.adapter.sse.ServerSentEvent
import retrofit2.adapter.sse.internal.AbstractSseCallAdapter

internal class SseJucFlowCallAdapter<ID : Any, TYPE : Any, DATA : Any>(
  executor: Executor?,
  retrofit: Retrofit,
  idType: Type,
  typeType: Type,
  dataType: Type,
) : AbstractSseCallAdapter<ID, TYPE, DATA, Flow.Publisher<ServerSentEvent<ID, TYPE, DATA>>>(retrofit, idType, typeType, dataType) {

  private val executor: Executor = executor ?: retrofit.callbackExecutor()
      ?: ForkJoinPool.commonPool().takeIf { ForkJoinPool.getCommonPoolParallelism() > 1 }
      ?: Executors.newCachedThreadPool()

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
      response.asSse(
        object : EventSourceListener() {
          override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            publisher.submit(createTypedEvent(id, type, data))
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

}
