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
package retrofit2.adapter.sse.internal

import java.lang.reflect.Type
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.sse.ServerSentEvent

private val EMPTY_ARRAY = emptyArray<Annotation>()

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : Any> conversionError(value: T, type: Type): Nothing = error("Failed to convert $value to $type, actual type is ${value.javaClass}")

abstract class AbstractSseCallAdapter<ID : Any, TYPE : Any, DATA : Any, O : Any, I : Any>(
  retrofit: Retrofit,
  private val idType: Type,
  private val typeType: Type,
  private val dataType: Type,
) : CallAdapter<ResponseBody, O> {

  private val idConverter: Converter<ResponseBody, ID?> = retrofit.responseBodyConverter(idType, EMPTY_ARRAY)
  private val typeConverter: Converter<ResponseBody, TYPE?> = retrofit.responseBodyConverter(typeType, EMPTY_ARRAY)
  private val dataConverter: Converter<ResponseBody, DATA> = retrofit.responseBodyConverter(dataType, EMPTY_ARRAY)

  final override fun responseType(): Type = ResponseBody::class.java

  protected fun Call<ResponseBody>.attachEventSourceListener(builder: I) {
    this.enqueue(
      object : retrofit2.Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
          response.asSse(
            object : EventSourceListener() {
              override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                emit(builder, createTypedEvent(id, type, data))
              }

              override fun onClosed(eventSource: EventSource) {
                close(builder)
              }

              override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                closeExceptionally(builder, t ?: RuntimeException()) // TODO: exception type
              }
            },
          )
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
          closeExceptionally(builder, t)
        }
      },
    )
  }

  private fun Response<ResponseBody>.asSse(listener: EventSourceListener) {
    val okhttpResponse = raw().newBuilder().body(body() ?: error("Response body is null")).build()
    EventSources.processResponse(okhttpResponse, listener)
  }

  private fun createTypedEvent(id: String?, type: String?, data: String): ServerSentEvent<ID, TYPE, DATA> {
    val convertedId = convertId(id)
    val convertedType = convertType(type)
    val convertedData = convertData(data)
    return ServerSentEvent(convertedId, convertedType, convertedData)
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

  protected abstract fun emit(builder: I, event: ServerSentEvent<ID, TYPE, DATA>)

  protected abstract fun close(builder: I)

  protected abstract fun closeExceptionally(builder: I, t: Throwable)
}
