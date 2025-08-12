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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.sse.EventSource
import retrofit2.adapter.sse.ServerSentEvent
import retrofit2.adapter.sse.SseCallback
import retrofit2.adapter.sse.internal.EventSourceCallAdapter

internal class SseKtxFlowCallAdapter<ID : Any, TYPE : Any, DATA : Any>(
  retrofit: Retrofit,
  idType: Type,
  typeType: Type,
  dataType: Type,
) : CallAdapter<ResponseBody, Flow<ServerSentEvent<ID, TYPE, DATA>>> {

  private val delegate = EventSourceCallAdapter<ID, TYPE, DATA>(
    retrofit,
    idType,
    typeType,
    dataType,
  )

  override fun responseType(): Type = delegate.responseType()

  override fun adapt(
    call: Call<ResponseBody>,
  ): Flow<ServerSentEvent<ID, TYPE, DATA>> {
    val delegate = delegate.adapt(call)
    return callbackFlow {
      delegate.subscribe(
        object : SseCallback<ID, TYPE, DATA> {
          override fun onEvent(
            eventSource: EventSource<ID, TYPE, DATA>,
            id: ID?,
            type: TYPE?,
            data: DATA,
          ) {
            trySendBlocking(ServerSentEvent(id, type, data))
          }

          override fun onClosed(eventSource: EventSource<ID, TYPE, DATA>) {
            close()
          }

          override fun onFailure(eventSource: EventSource<ID, TYPE, DATA>, t: Throwable?) {
            close(t ?: RuntimeException()) // TODO exception type
          }
        },
      )

      awaitClose(call::cancel)
    }
  }

}
