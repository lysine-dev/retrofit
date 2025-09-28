package test

import retrofit2.*
import retrofit2.http.*

internal class Body

internal interface Service {
  @GET("/")
  suspend fun get(c: Body): Any
}
