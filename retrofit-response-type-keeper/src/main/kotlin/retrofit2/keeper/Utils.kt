/*
 * Copyright (C) 2025 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

internal fun keepRuleForType(referencedType: String): String =
  "-keep,allowoptimization,allowshrinking,allowobfuscation class $referencedType\n"

internal fun proguardFilePath(typeName: String) =
  "META-INF/proguard/retrofit-response-type-keeper-$typeName.pro"
