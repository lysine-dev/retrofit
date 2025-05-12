---
title: Configuration
---

`Retrofit` is the class through which your API interfaces are turned into callable objects.
By default, Retrofit will give you sane defaults for your platform, but it allows for customization.

## Converters

By default, Retrofit can only deserialize HTTP bodies into OkHttp's `ResponseBody` type, and it can only accept its `RequestBody` type for `@Body`.

Converters can be added to support other types. Sibling modules adapt popular serialization libraries for your convenience.

### Built-in converters

* [Gson](https://github.com/google/gson): `com.squareup.retrofit2:converter-gson`
* [Jackson](https://github.com/FasterXML/jackson): `com.squareup.retrofit2:converter-jackson`
* [Moshi](https://github.com/square/moshi/): `com.squareup.retrofit2:converter-moshi`
* [Protobuf](https://developers.google.com/protocol-buffers/): `com.squareup.retrofit2:converter-protobuf`
* [Wire](https://github.com/square/wire): `com.squareup.retrofit2:converter-wire`
* [Simple XML](http://simple.sourceforge.net/): `com.squareup.retrofit2:converter-simplexml`
* [JAXB](https://docs.oracle.com/javase/tutorial/jaxb/intro/index.html): `com.squareup.retrofit2:converter-jaxb`
* [Kotlin serialization](https://github.com/Kotlin/kotlinx.serialization/): `com.squareup.retrofit2:converter-kotlinx-serialization`
* Scalars (primitives, boxed, and String): `com.squareup.retrofit2:converter-scalars`

Here's an example of using the `GsonConverterFactory` class to generate an implementation of the `GitHubService` interface which uses Gson for its deserialization.

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.github.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build();

GitHubService service = retrofit.create(GitHubService.class);
```

### Delegating converters

Delegating converters differ from the converters above in that they don't actually convert bytes to object.
Instead, they delegate to another converter, and then wrap the potentially-null result into an optional.

Two delegating converters are provided:

* Guava's `Optional<T>` - `com.squareup.retrofit2:converter-guava`
* Java 8's `Optional<T>` - `com.squareup.retrofit2:converter-java8`

### Custom converters

If you need to communicate with an API that uses a content-format that Retrofit does not support out of the box (e.g. YAML, txt, custom format) or you wish to use a different library to implement an existing format, you can easily create your own converter. Create a class that extends the [`Converter.Factory` class](https://github.com/square/retrofit/blob/master/retrofit/src/main/java/retrofit2/Converter.java) and pass in an instance when building your adapter.

### Third-party converters

Various third-party converters have been created by the community for other libraries and serialization formats:

* [MessagePack](https://github.com/komamitsu/retrofit-converter-msgpack) - `org.komamitsu:retrofit-converter-msgpack`
* [LoganSquare](https://github.com/aurae/retrofit-logansquare) - `com.github.aurae.retrofit2:converter-logansquare`
* [FastJson](https://github.com/ZYRzyr/FastJsonConverter) - `com.github.ZYRzyr:FastJsonConverter`
* [FastJson](https://github.com/ligboy/retrofit-converter-fastjson) - `org.ligboy.retrofit2:converter-fastjson` or `org.ligboy.retrofit2:converter-fastjson-android`
* [Thrifty](https://github.com/infinum/thrifty-retrofit-converter) - `co.infinum:retrofit-converter-thrifty`
* [jspoon](https://github.com/DroidsOnRoids/jspoon/tree/master/retrofit-converter-jspoon) (HTML)- `pl.droidsonroids.retrofit2:converter-jspoon`
* [Fruit](https://github.com/ghuiii/Fruit/tree/master/converter-retrofit) - `me.ghui:fruit-converter-retrofit`
* [JakartaEE JsonB](https://github.com/cchacin/jsonb-retrofit-converter/) - `io.github.cchacin:jsonb-retrofit-converter`


## Call adapters

Retrofit is pluggable allowing different execution mechanisms and their libraries to be used for performing the HTTP call.
This allows API requests to seamlessly compose with any existing threading model and/or task framework in the rest of your app.

### Built-in call adapters

* [RxJava `Observable` & `Single`](https://github.com/ReactiveX/RxJava/) - `com.squareup.retrofit2:adapter-rxjava`
* [RxJava2 `Observable`, `Flowable`, `Single`, `Completable` & `Maybe`](https://github.com/ReactiveX/RxJava/) - `com.squareup.retrofit2:adapter-rxjava2`
* [RxJava3 `Observable`, `Flowable`, `Single`, `Completable` & `Maybe`](https://github.com/ReactiveX/RxJava/) - `com.squareup.retrofit2:adapter-rxjava3`
* [Guava `ListenableFuture`](https://github.com/google/guava/) - `com.squareup.retrofit2:adapter-guava`
* [Java 8 `CompletableFuture`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) - `com.squareup.retrofit2:adapter-java8`
* [Kotlin `suspend` functions] - No dependency needed!

### Custom call adapters

If you need to integration with a work library that Retrofit does not support out of the box, or you wish to use a different strategy to adapt an existing library, you can easily create your own call adapter.
Create a class that extends the [`CallAdapter.Factory` class](https://github.com/square/retrofit/blob/master/retrofit/src/main/java/retrofit2/CallAdapter.java) for a target type, and return an adapter which wraps the built-in `Call`.

### Third-party call adapters

Various third-party adapters have been created by the community for other libraries:

* [Bolts](https://github.com/zeng1990java/retrofit-bolts-call-adapter)
* [Agera](https://github.com/drakeet/retrofit-agera-call-adapter)
* [Project Reactor](https://github.com/JakeWharton/retrofit2-reactor-adapter)
