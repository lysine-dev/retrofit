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
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.sse.ServerSentEvent
import retrofit2.adapter.sse.internal.AbstractSseCallAdapter

internal class SseJucFlowCallAdapter<ID : Any, TYPE : Any, DATA : Any>(
  executor: Executor?,
  retrofit: Retrofit,
  idType: Type,
  typeType: Type,
  dataType: Type,
) : AbstractSseCallAdapter<ID, TYPE, DATA, Flow.Publisher<ServerSentEvent<ID, TYPE, DATA>>, SubmissionPublisher<ServerSentEvent<ID, TYPE, DATA>>>(retrofit, idType, typeType, dataType) {

  private val executor: Executor = executor ?: retrofit.callbackExecutor()
    ?: ForkJoinPool.commonPool().takeIf { ForkJoinPool.getCommonPoolParallelism() > 1 }
    ?: Executors.newCachedThreadPool()

  override fun adapt(
    call: Call<ResponseBody>,
  ): Flow.Publisher<ServerSentEvent<ID, TYPE, DATA>> {
    return object : SubmissionPublisher<ServerSentEvent<ID, TYPE, DATA>>(executor, Flow.defaultBufferSize()) {
      override fun subscribe(subscriber: Flow.Subscriber<in ServerSentEvent<ID, TYPE, DATA>>?) {
        super.subscribe(subscriber)
        call.attachEventSourceListener(this)
      }

      override fun close() {
        call.cancel()
        super.close()
      }
    }
  }

  override fun emit(
    builder: SubmissionPublisher<ServerSentEvent<ID, TYPE, DATA>>,
    event: ServerSentEvent<ID, TYPE, DATA>,
  ) {
    builder.submit(event)
  }

  override fun close(builder: SubmissionPublisher<ServerSentEvent<ID, TYPE, DATA>>) {
    builder.close()
  }

  override fun closeExceptionally(
    builder: SubmissionPublisher<ServerSentEvent<ID, TYPE, DATA>>,
    t: Throwable,
  ) {
    builder.closeExceptionally(t)
  }
}
