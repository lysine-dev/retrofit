package test

import retrofit2.*
import retrofit2.http.*

internal class DeleteUser
internal class GetUser
internal class HeadUser
internal class HttpUser
internal class OptionsUser
internal class PatchUser
internal class PostUser
internal class PutUser

internal interface Service {
  @DELETE("/")
  fun delete(): Call<DeleteUser>

  @GET("/")
  fun get(): Call<GetUser>

  @HEAD("/")
  fun head(): Call<HeadUser>

  @retrofit2.http.HTTP(method = "CUSTOM", path = "/")
  fun http(): Call<HttpUser>

  @OPTIONS("/")
  fun options(): Call<OptionsUser>

  @PATCH("/")
  fun patch(): Call<PatchUser>

  @POST("/")
  fun post(): Call<PostUser>

  @PUT("/")
  fun put(): Call<PutUser>
}
