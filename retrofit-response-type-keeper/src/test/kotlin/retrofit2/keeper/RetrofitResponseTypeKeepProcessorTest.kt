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
import javax.tools.StandardLocation.CLASS_OUTPUT
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class RetrofitResponseTypeKeepProcessorTest(
  @param:TestParameter private val processor: Processor,
) {
  @Test
  fun allHttpMethods() {
    val rules = """
      |# test.Service
      |-keep,allowoptimization,allowshrinking,allowobfuscation class retrofit2.Call
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.DeleteUser
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.GetUser
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.HeadUser
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.HttpUser
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.OptionsUser
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.PatchUser
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.PostUser
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.PutUser
      |
    """.trimMargin()

    when (processor) {
      Processor.Apt -> {
        val source = """
          package test;

          import retrofit2.*;
          import retrofit2.http.*;

          class DeleteUser {}
          class GetUser {}
          class HeadUser {}
          class HttpUser {}
          class OptionsUser {}
          class PatchUser {}
          class PostUser {}
          class PutUser {}

          interface Service {
            @DELETE("/") Call<DeleteUser> delete();
            @GET("/") Call<GetUser> get();
            @HEAD("/") Call<HeadUser> head();
            @HTTP(method = "CUSTOM", path = "/") Call<HttpUser> http();
            @OPTIONS("/") Call<OptionsUser> options();
            @PATCH("/") Call<PatchUser> patch();
            @POST("/") Call<PostUser> post();
            @PUT("/") Call<PutUser> put();
          }
        """.trimIndent()
        processor.validate(source, rules)
      }

      Processor.Ksp -> {
        val source = """
          package test

          import retrofit2.*
          import retrofit2.http.*

          class DeleteUser
          class GetUser
          class HeadUser
          class HttpUser
          class OptionsUser
          class PatchUser
          class PostUser
          class PutUser

          interface Service {
            @DELETE("/")
            fun delete(): Call<DeleteUser>

            @GET("/")
            fun get(): Call<GetUser>

            @HEAD("/")
            fun head(): Call<HeadUser>

            @retrofit2.http.HTTP(method = "CUSTOM", path = "/")
            fun http(): Call<HttpUser>

            @OPTIONS("/")
            fun options(): Call<OptionsUser>

            @PATCH("/")
            fun patch(): Call<PatchUser>

            @POST("/")
            fun post(): Call<PostUser>

            @PUT("/")
            fun put(): Call<PutUser>
          }
        """.trimIndent()
        processor.validate(source, rules)
      }
    }
  }

  @Test
  fun nesting() {
    val rules = """
      |# test.Service
      |-keep,allowoptimization,allowshrinking,allowobfuscation class retrofit2.Call
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.One
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.Three
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.Two
      |
    """.trimMargin()
    when (processor) {
      Processor.Apt -> {
        val source = """
          package test;

          import retrofit2.*;
          import retrofit2.http.*;

          class One<T> {}
          class Two<T> {}
          class Three {}

          interface Service {
            @GET("/") Call<One<Two<Three>>> get();
          }
        """.trimIndent()
        processor.validate(source, rules)
      }

      Processor.Ksp -> {
        val source = """
          package test

          import retrofit2.*
          import retrofit2.http.*

          internal class One<T>
          internal class Two<T>
          internal class Three

          internal interface Service {
            @GET("/")
            fun get(): Call<One<Two<Three>>>
          }
        """.trimIndent()
        processor.validate(source, rules)
      }
    }
  }

  @Test
  fun kotlinSuspend() {
    val rules = """
      |# test.Service
      |-keep,allowoptimization,allowshrinking,allowobfuscation class java.lang.Object
      |-keep,allowoptimization,allowshrinking,allowobfuscation class test.Body
      |
    """.trimMargin()
    when (processor) {
      Processor.Apt -> {
        val source = """
          package test;

          import kotlin.coroutines.Continuation;
          import retrofit2.*;
          import retrofit2.http.*;

          class Body {}

          interface Service {
            @GET("/") Object get(Continuation<? extends Body> c);
          }
        """.trimIndent()
        processor.validate(source, rules)
      }

      Processor.Ksp -> {
        val source = """
          package test

          import retrofit2.*
          import retrofit2.http.*

          internal class Body

          internal interface Service {
            @GET("/")
            suspend fun get(c: Body): Any
          }
        """.trimIndent()
        processor.validate(source, rules)
      }
    }
  }

  enum class Processor {
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
}
