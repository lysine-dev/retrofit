# Keep generic signature of RxJava3 (R8 full mode strips signatures from non-kept items).
# It's necessary to add the explicit rule for Result as it could be used as a nested return type like `Observable<Result<String>>`
-keep,allowobfuscation,allowshrinking class retrofit2.adapter.rxjava3.Result
