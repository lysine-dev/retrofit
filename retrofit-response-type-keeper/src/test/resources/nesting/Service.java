package test;

import retrofit2.*;
import retrofit2.http.*;

class One<T> {}
class Two<T> {}
class Three {}

interface Service {
  @GET("/") Call<One<Two<Three>>> get();
}
