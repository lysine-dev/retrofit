package test

import retrofit2.*
import retrofit2.http.*

internal class One<T>
internal class Two<T>
internal class Three

internal interface Service {
  @GET("/")
  fun get(): Call<One<Two<Three>>>
}
