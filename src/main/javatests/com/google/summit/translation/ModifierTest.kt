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
import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.modifier.AnnotationModifier
import com.google.summit.ast.modifier.Modifier
import com.google.summit.ast.traversal.DfsWalker
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModifierTest {

  private fun classHasAnnotationNamed(cu: CompilationUnit, name: String): Boolean {
    return cu.typeDeclaration
      .modifiers
      .filter { (it as? AnnotationModifier)?.name?.asCodeString() == name }
      .isNotEmpty()
  }

  @Test
  fun classDeclaration_translation_hasCorrectAnnotations() {
    val cu =
      TranslateHelpers.parseAndTranslate(
        """
        @isTest
        @JsonAccess(serializable='samePackage' deserializable='sameNamespace')
        class Test { }
        """
      )

    assertThat(classHasAnnotationNamed(cu, "isTest")).isTrue()
    assertThat(classHasAnnotationNamed(cu, "JsonAccess")).isTrue()
    assertThat(classHasAnnotationNamed(cu, "serializable")).isFalse()
  }

  @Test
  fun every_keywordModifier_isTranslated() {
    val keywordList =
      listOf(
        "public",
        "private",
        "protected",
        "abstract",
        "final",
        "global",
        "inherited sharing",
        "override",
        "static",
        "testMethod",
        "transient",
        "virtual",
        "webservice",
        "with sharing",
        "without sharing"
      )

    for (modifier in keywordList) {
      // Not all of these modifiers are semantically valid for a class declaration,
      // but they are accepted by the parser and translated.
      val cu = TranslateHelpers.parseAndTranslate("$modifier class Test { }")

      assertThat(cu.typeDeclaration.modifiers).hasSize(1)
    }
  }

  @Test
  fun modifiers_linked_inAST() {
    val cu =
      TranslateHelpers.parseAndTranslate(
        """
        @isTest
        public class Test { }
        """
      )

    assertThat(DfsWalker(cu).stream().filter { it is Modifier }.count()).isEqualTo(2)
  }
}
