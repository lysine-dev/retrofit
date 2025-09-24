# Keep generic signature of Optional (R8 full mode strips signatures from non-kept items).
-keep,allowoptimization,allowshrinking,allowobfuscation class com.google.common.base.Optional
