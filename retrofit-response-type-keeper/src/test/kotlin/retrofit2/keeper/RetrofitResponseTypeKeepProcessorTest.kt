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
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.NoSuchFileException
import javax.tools.StandardLocation.CLASS_OUTPUT
import kotlin.io.path.readText
import kotlin.io.path.toPath
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class RetrofitResponseTypeKeepProcessorTest {
  @Test
  fun process(
    @TestParameter(
      "all-http-methods",
      "nesting",
      "kotlin-suspend",
    ) name: String
  ) {
    val service = JavaFileObjects.forSourceString(
      "test.Service",
      readResourceAsText("$name/Service.java"),
    )

    assertAbout(javaSource())
      .that(service)
      .processedWith(RetrofitResponseTypeKeepProcessor())
      .compilesWithoutError()
      .and()
      .generatesFileNamed(
        CLASS_OUTPUT,
        "",
        "META-INF/proguard/retrofit-response-type-keeper-test.Service.pro",
      ).withStringContents(
        UTF_8,
        readResourceAsText("$name/Service.pro"),
      )
  }

  private companion object {
    fun readResourceAsText(name: String): String {
      val resource = this::class.java.classLoader.getResource(name)
        ?: throw NoSuchFileException("Resource $name not found.")
      return resource.toURI().toPath().readText()
    }
  }
}
