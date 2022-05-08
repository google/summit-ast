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
import com.google.summit.ast.declaration.EnumDeclaration
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CompilationUnitTest {

  @Test
  fun enum_translation_hasClassDeclaration() {
    val cu = TranslateHelpers.parseAndTranslate("enum Test { }")

    assertThat(cu.typeDeclaration).isInstanceOf(EnumDeclaration::class.java)
  }

  @Test
  fun parent_reverses_getChildren() {
    val cu = TranslateHelpers.parseAndTranslate("class Test { }")
    val classDecl = cu.typeDeclaration

    assertThat(cu.getChildren()).containsExactly(classDecl)
    assertThat(classDecl.parent).isEqualTo(cu)

    assertWithMessage("CompilationUnits are the root of the AST and should have no parent")
      .that(cu.parent)
      .isNull()
  }
}
