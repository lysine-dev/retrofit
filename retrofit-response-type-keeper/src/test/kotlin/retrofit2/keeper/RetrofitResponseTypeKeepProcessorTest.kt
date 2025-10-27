/*
 * Copyright (C) 2024 Square, Inc.
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

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.NoSuchFileException
import javax.tools.StandardLocation.CLASS_OUTPUT
import kotlin.io.path.readText
import kotlin.io.path.toPath
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class RetrofitResponseTypeKeepProcessorTest(
  @param:TestParameter private val generator: Generator,
) {
  @Test
  fun process(
    @TestParameter(
      "all-http-methods",
      "nesting",
      "kotlin-suspend",
    ) name: String,
  ) {
    val rules = readResourceAsText("$name/Service.pro")

    when (generator) {
      Generator.Apt -> {
        val source = readResourceAsText("$name/Service.java")
        generator.validate(source, rules)
      }

      Generator.Ksp -> {
        val source = readResourceAsText("$name/Service.kt")
        generator.validate(source, rules)
      }
    }
  }

  enum class Generator {
    Apt {
      override fun validate(source: String, rules: String) {
        val service = JavaFileObjects.forSourceString("test.Service", source)
        assertAbout(javaSource())
          .that(service)
          .processedWith(RetrofitResponseTypeKeepProcessor())
          .compilesWithoutError()
          .and()
          .generatesFileNamed(
            CLASS_OUTPUT,
            "",
            GENERATED_PATH,
          ).withStringContents(
            UTF_8,
            rules,
          )
      }
    },

    Ksp {
      @OptIn(ExperimentalCompilerApi::class)
      override fun validate(source: String, rules: String) {
        val compilation = KotlinCompilation().apply {
          configureKsp {
            inheritClassPath = true
            symbolProcessorProviders += RetrofitResponseTypeKeepSymbolProcessor.Provider()
            sources = listOf(SourceFile.new("Service.kt", source))
          }
        }
        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        assertThat(compilation.kspSourcesDir.resolve("resources/$GENERATED_PATH").readText())
          .isEqualTo(rules)
      }
    },
    ;

    abstract fun validate(source: String, rules: String)

    private companion object {
      const val GENERATED_PATH = "META-INF/proguard/retrofit-response-type-keeper-test.Service.pro"
    }
  }

  private companion object {
    fun readResourceAsText(name: String): String {
      val resource = this::class.java.classLoader.getResource(name)
        ?: throw NoSuchFileException("Resource $name not found.")
      return resource.toURI().toPath().readText()
    }
  }
}
