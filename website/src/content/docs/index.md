---
title: Introduction
---

Retrofit turns your HTTP API into a Java (or Kotlin) interface.

```java
public interface GitHubService {
  @GET("users/{user}/repos")
  Call<List<Repo>> listRepos(@Path("user") String user);
}
```

The `Retrofit` class generates an implementation of the `GitHubService` interface.

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.github.com")
    .build();

GitHubService service = retrofit.create(GitHubService.class);
```

Each `Call` from the created `GitHubService` can make synchronous or asynchronous HTTP requests to the remote webserver.

```java
Call<List<Repo>> repos = service.listRepos("octocat");
```

Use annotations to describe the HTTP request on each interface method:

* URL parameter replacement and query parameter support
* Object conversion to request body (e.g., JSON, protocol buffers)
* Multipart request body and file upload
