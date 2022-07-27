/*
 * Copyright 2022 Google LLC
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

package com.google.summit.translation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.summit.ast.declaration.MethodDeclaration
import com.google.summit.ast.modifier.KeywordModifier
import com.google.summit.testing.TranslateHelpers
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MethodDeclarationTest {

  @Test
  fun method_translation_hasMethodDeclaration() {
    val input = "class Test { String doNothing(String [] input) { return input[0]; } }"

    val methodDecl = TranslateHelpers.parseAndFindFirstNodeOfType<MethodDeclaration>(input)

    assertNotNull(methodDecl)
    assertThat(methodDecl.qualifiedName).isEqualTo("Test.doNothing")
    assertWithMessage("Method should return a String")
      .that(methodDecl.returnType.asCodeString())
      .isEqualTo("String")
    assertWithMessage("Method should not return void")
      .that(methodDecl.returnType.isVoid())
      .isFalse()
    assertWithMessage("Method should have 1 parameter")
      .that(methodDecl.parameterDeclarations)
      .hasSize(1)
    val param = methodDecl.parameterDeclarations.first()
    assertWithMessage("Parameter should be named 'input'")
      .that(param.name.asCodeString())
      .isEqualTo("input")
    assertWithMessage("Parameter should be a String array type")
      .that(param.type.asCodeString())
      .isEqualTo("String[]")
  }

  @Test
  fun voidMethodWithoutParameters_translates_correctly() {
    val input = "class Test { void doNothing() { } }"

    val methodDecl = TranslateHelpers.parseAndFindFirstNodeOfType<MethodDeclaration>(input)

    assertNotNull(methodDecl)
    assertWithMessage("Method should return void").that(methodDecl.returnType.isVoid()).isTrue()
    assertWithMessage("Method should return void")
      .that(methodDecl.returnType.asCodeString())
      .isEqualTo("void")
    assertWithMessage("Method should have no parameters")
      .that(methodDecl.parameterDeclarations)
      .isEmpty()
  }

  @Test
  fun modifiers_translated_onMethodsAndParameters() {
    val input = "class Test { public void method(final int x) { } }"

    val methodDecl = TranslateHelpers.parseAndFindFirstNodeOfType<MethodDeclaration>(input)

    assertNotNull(methodDecl)
    assertWithMessage("Method should have 1 modifier").that(methodDecl.modifiers).hasSize(1)
    assertWithMessage("Method should have 'public' modifier")
      .that(methodDecl.hasKeyword(KeywordModifier.Keyword.PUBLIC))
      .isTrue()

    val parameterDecl = methodDecl.parameterDeclarations.first()
    assertWithMessage("Parameter should have 1 modifier").that(parameterDecl.modifiers).hasSize(1)
    assertWithMessage("Parameter should have 'final' modifier")
      .that(parameterDecl.hasKeyword(KeywordModifier.Keyword.FINAL))
      .isTrue()
  }
}
