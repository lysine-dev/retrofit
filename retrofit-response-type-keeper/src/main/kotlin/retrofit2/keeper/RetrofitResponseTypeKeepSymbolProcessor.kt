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
      val typeName = requireNotNull(element.qualifiedName).asString()

      val dependencies = Dependencies(aggregating = false, containingFile)
      codeGenerator.createNewFile(dependencies, "", outputFile(typeName), "")
        .bufferedWriter().use { writer ->
          writer.write("# $typeName\n")
          for (referencedType in referencedTypes.sorted()) {
            writer.write("$KEEP_RULE_PREFIX $referencedType\n")
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
