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

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

class RetrofitResponseTypeKeepSymbolProcessor(
  environment: SymbolProcessorEnvironment,
) : SymbolProcessor {
  private val codeGenerator: CodeGenerator = environment.codeGenerator

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val elementToReferencedTypes = mutableMapOf<KSClassDeclaration, MutableSet<String>>()

    annotationNames.flatMap { resolver.getSymbolsWithAnnotation(it) }
      .filterIsInstance<KSFunctionDeclaration>()
      .forEach { function ->
        val serviceType = function.parentDeclaration as? KSClassDeclaration ?: return@forEach
        val referenced = elementToReferencedTypes.getOrPut(serviceType, ::LinkedHashSet)

        // Retrofit has special support for 'suspend fun' in Kotlin which manifests as a
        // final Continuation parameter whose generic type is the declared return type.
        if (function.modifiers.contains(Modifier.SUSPEND)) {
          function.parameters.forEach {
            it.type.resolve().recursiveParameterizedTypesTo(referenced)
          }
        }

        val returnType = function.returnType?.resolve() ?: return@forEach
        returnType.recursiveParameterizedTypesTo(referenced)
      }

    elementToReferencedTypes.forEach { (element, referencedTypes) ->
      val containingFile = element.containingFile ?: return@forEach
      val typeName = element.qualifiedName?.asString() ?: return@forEach

      val dependencies = Dependencies(aggregating = false, containingFile)
      codeGenerator.createNewFile(dependencies, "", proguardFilePath(typeName), "")
        .bufferedWriter().use { w ->
          w.write("# $typeName\n")
          for (referencedType in referencedTypes.sorted()) {
            w.write(keepRuleForType(referencedType))
          }
        }
    }

    return emptyList()
  }

  private fun KSType.recursiveParameterizedTypesTo(types: MutableSet<String>) {
    val declaration = this.declaration
    if (declaration is KSClassDeclaration) {
      var qualifiedName = declaration.qualifiedName?.asString()
      if (qualifiedName == "kotlin.Any") {
        qualifiedName = "java.lang.Object"
      }
      qualifiedName?.let { types.add(it) }
    }

    for (typeArgument in arguments) {
      typeArgument.type?.resolve()?.recursiveParameterizedTypesTo(types)
    }
  }

  @Suppress("unused") // Used in service file.
  @AutoService(SymbolProcessorProvider::class)
  class Provider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
      RetrofitResponseTypeKeepSymbolProcessor(environment)
  }
}
