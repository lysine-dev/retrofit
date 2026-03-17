package test;

import retrofit2.*;
import retrofit2.http.*;

class DeleteUser {}
class GetUser {}
class HeadUser {}
class HttpUser {}
class OptionsUser {}
class PatchUser {}
class PostUser {}
class PutUser {}

interface Service {
  @DELETE("/") Call<DeleteUser> delete();
  @GET("/") Call<GetUser> get();
  @HEAD("/") Call<HeadUser> head();
  @HTTP(method = "CUSTOM", path = "/") Call<HttpUser> http();
  @OPTIONS("/") Call<OptionsUser> options();
  @PATCH("/") Call<PatchUser> patch();
  @POST("/") Call<PostUser> post();
  @PUT("/") Call<PutUser> put();
}
