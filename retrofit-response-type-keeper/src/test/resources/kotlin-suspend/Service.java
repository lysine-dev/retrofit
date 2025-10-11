package test;

import kotlin.coroutines.Continuation;
import retrofit2.*;
import retrofit2.http.*;

class Body {}

interface Service {
  @GET("/") Object get(Continuation<? extends Body> c);
}
