package retrofit2.keeper

internal val annotationNames = setOf(
  "retrofit2.http.DELETE",
  "retrofit2.http.GET",
  "retrofit2.http.HEAD",
  "retrofit2.http.HTTP",
  "retrofit2.http.OPTIONS",
  "retrofit2.http.PATCH",
  "retrofit2.http.POST",
  "retrofit2.http.PUT",
)

internal const val KEEP_RULE_PREFIX = "-keep,allowoptimization,allowshrinking,allowobfuscation class"

internal fun outputFile(typeName: String) =
  "META-INF/proguard/retrofit-response-type-keeper-$typeName.pro"
