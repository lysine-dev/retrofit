---
title: Declarations
---

Annotations on the interface methods and its parameters indicate how a request will be handled.

## Request method

Every method must have an HTTP annotation that provides the request method and relative URL. There are eight built-in annotations: `HTTP`, `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`, and `HEAD`. The relative URL of the resource is specified in the annotation.

```java
@GET("users/list")
```

You can also specify static query parameters directly on the relative URL.

```java
@GET("users/list?sort=desc")
```

## URL manipulation

A request URL can be updated dynamically using replacement blocks and parameters on the method. A replacement block is an alphanumeric string surrounded by `{` and `}`. A corresponding parameter must be annotated with `@Path` using the same string.

```java
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId);
```

Query parameters can also be added.

```java
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId, @Query("sort") String sort);
```

For complex query parameter combinations a `Map` can be used.

```java
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId, @QueryMap Map<String, String> options);
```

## Request body

An object can be specified for use as an HTTP request body with the `@Body` annotation.

```java
@POST("users/new")
Call<User> createUser(@Body User user);
```

The object will also be converted using a converter specified on the `Retrofit` instance. If no converter is added, only `RequestBody` can be used.

## Form-encoded and multipart

Methods can also be declared to send form-encoded and multipart data.
Form-encoded data is sent when `@FormUrlEncoded` is present on the method. Each key-value pair is annotated with `@Field` containing the name and the object providing the value.

```java
@FormUrlEncoded
@POST("user/edit")
Call<User> updateUser(@Field("first_name") String first, @Field("last_name") String last);
```

Multipart requests are used when `@Multipart` is present on the method. Parts are declared using the `@Part` annotation.

```java
@Multipart
@PUT("user/photo")
Call<User> updateUser(@Part("photo") RequestBody photo, @Part("description") RequestBody description);
```

Multipart parts use one of `Retrofit`'s converters, or they can implement `RequestBody` to handle their own serialization.

## Header manipulation

You can set static headers for a method using the `@Headers` annotation.

```java
@Headers("Cache-Control: max-age=640000")
@GET("widget/list")
Call<List<Widget>> widgetList();
```

```java
@Headers({
"Accept: application/vnd.github.v3.full+json",
"User-Agent: Retrofit-Sample-App"
})
@GET("users/{username}")
Call<User> getUser(@Path("username") String username);
```

Note that headers do not overwrite each other. All headers with the same name will be included in the request.

A request Header can be updated dynamically using the `@Header` annotation. A corresponding parameter must be provided to the `@Header`. If the value is null, the header will be omitted. Otherwise, `toString` will be called on the value, and the result used.

```java
@GET("user")
Call<User> getUser(@Header("Authorization") String authorization)
```

Similar to query parameters, for complex header combinations, a `Map` can be used.

```java
@GET("user")
Call<User> getUser(@HeaderMap Map<String, String> headers)
```

Headers that need to be added to every request can be specified using an [OkHttp interceptor](https://square.github.io/okhttp/features/interceptors/).

## Synchronous vs. asynchronous

`Call` instances can be executed either synchronously or asynchronously. Each instance can only be used once, but calling `clone()` will create a new instance that can be used.

On Android, callbacks will be executed on the main thread. On the JVM, callbacks will happen on the same thread that executed the HTTP request.

## Kotlin support

Interface methods support kotlin suspend functions which directly return a `Response` object, creating and asynchronously executing the call while suspending the current function.

```kotlin
@GET("users")
suspend fun getUser(): Response<User>
```

A suspend method may also directly return the body. If a non-2XX status is returned an `HttpException` will be thrown containing the response.

```kotlin
@GET("users")
suspend fun getUser(): User
```
