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
import com.google.summit.ast.declaration.InterfaceDeclaration
import com.google.summit.ast.declaration.MethodDeclaration
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InterfaceDeclarationTest {

  @Test
  fun interface_translation_hasInterfaceDeclaration() {
    val cu = TranslateHelpers.parseAndTranslate("interface Test { }")

    assertThat(cu.typeDeclaration).isInstanceOf(InterfaceDeclaration::class.java)
    val interfaceDecl = cu.typeDeclaration as InterfaceDeclaration
    assertWithMessage("Interface should have no super interfaces")
      .that(interfaceDecl.extendsTypes)
      .isEmpty()
  }

  @Test
  fun interface_translation_includesInheritance() {
    val cu = TranslateHelpers.parseAndTranslate("interface Test extends I1, I2 { }")
    val interfaceDecl = cu.typeDeclaration as InterfaceDeclaration

    assertThat(interfaceDecl.extendsTypes.map { it.asCodeString() }).containsExactly("I1", "I2")
  }

  @Test
  fun methodInInterface_has_methodDeclaration() {
    val input = "interface Test { String reverse(String name); }"

    val methodDecl = TranslateHelpers.parseAndFindFirstNodeOfType<MethodDeclaration>(input)

    assertNotNull(methodDecl)
    assertThat(methodDecl.qualifiedName).isEqualTo("Test.reverse")
    assertThat(methodDecl.returnType.asCodeString()).isEqualTo("String")
    assertWithMessage("Method should have 1 parameter")
      .that(methodDecl.parameterDeclarations)
      .hasSize(1)
  }

  @Test
  fun methodReturningVoid_translation_hasVoidReturnType() {
    val input = "interface Test { void doNothing(); }"

    val methodDecl = TranslateHelpers.parseAndFindFirstNodeOfType<MethodDeclaration>(input)

    assertNotNull(methodDecl)
    assertThat(methodDecl.qualifiedName).isEqualTo("Test.doNothing")
    assertWithMessage("Method should return void").that(methodDecl.returnType.isVoid()).isTrue()
    assertWithMessage("Method should have no parameters")
      .that(methodDecl.parameterDeclarations)
      .isEmpty()
  }
}
