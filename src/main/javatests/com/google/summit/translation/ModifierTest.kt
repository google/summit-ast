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
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.modifier.AnnotationModifier
import com.google.summit.ast.modifier.ElementValue
import com.google.summit.ast.modifier.KeywordModifier
import com.google.summit.ast.modifier.Modifier
import com.google.summit.ast.traversal.DfsWalker
import com.google.summit.testing.TranslateHelpers
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModifierTest {

  private fun findAnnotationOnClass(cu: CompilationUnit, name: String): AnnotationModifier? =
    cu.typeDeclaration.modifiers.filterIsInstance<AnnotationModifier>().find {
      it.name.asCodeString() == name
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

    assertThat(findAnnotationOnClass(cu, "isTest")).isNotNull()
    assertThat(findAnnotationOnClass(cu, "JsonAccess")).isNotNull()
    assertThat(findAnnotationOnClass(cu, "serializable")).isNull()
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

  @Test
  fun annotation_arguments_areCorrectlyIdentified() {
    val cu =
      TranslateHelpers.parseAndTranslate(
        """
          @A(label='X' description='Y' category='Z')
          @B(false)
          @C({1, 2, 3})
          @D(cacheable=true)
          public class Test { }
        """
      )

    val annotationA = findAnnotationOnClass(cu, "A")!!
    val annotationB = findAnnotationOnClass(cu, "B")!!
    val annotationC = findAnnotationOnClass(cu, "C")!!
    val annotationD = findAnnotationOnClass(cu, "D")!!

    assertThat(annotationA.args).hasSize(3)
    assertThat(annotationA.args.none { it.isNameImplicit }).isTrue()

    assertThat(annotationB.args).hasSize(1)
    assertThat(annotationB.args[0].isNameImplicit).isTrue()

    assertThat(annotationC.args).hasSize(1)
    assertThat(annotationC.args[0].isNameImplicit).isTrue()

    assertThat(annotationD.args).hasSize(1)
    assertThat(annotationD.args[0].isNameImplicit).isFalse()
  }

  @Test
  fun annotation_arguments_areCorrectlyParsed() {
    val cu =
      TranslateHelpers.parseAndTranslate(
        """
          @A(@X)
          @B({@Y, @Z})
          @C(a = false, b = {1, 2, 3}, c = @d)
          @D
          @E()
          public class Test { }
        """
      )

    val annotationA = findAnnotationOnClass(cu, "A")!!
    val annotationB = findAnnotationOnClass(cu, "B")!!
    val annotationC = findAnnotationOnClass(cu, "C")!!
    val annotationD = findAnnotationOnClass(cu, "D")!!
    val annotationE = findAnnotationOnClass(cu, "E")!!

    assertThat(annotationA.args).hasSize(1)
    assertThat(annotationA.args[0].value).isInstanceOf(ElementValue.AnnotationValue::class.java)

    assertThat(annotationB.args).hasSize(1)
    assertThat(annotationB.args[0].value).isInstanceOf(ElementValue.ArrayValue::class.java)
    val annotationB_array = annotationB.args[0].value as ElementValue.ArrayValue
    assertThat(annotationB_array.values).hasSize(2)
    annotationB_array.values.forEach {
      assertThat(it).isInstanceOf(ElementValue.AnnotationValue::class.java)
    }

    assertThat(annotationC.args).hasSize(3)
    assertThat(annotationC.args[0].name.asCodeString()).isEqualTo("a")
    assertThat(annotationC.args[0].value).isInstanceOf(ElementValue.ExpressionValue::class.java)
    assertThat(annotationC.args[1].name.asCodeString()).isEqualTo("b")
    assertThat(annotationC.args[1].value).isInstanceOf(ElementValue.ArrayValue::class.java)
    assertThat(annotationC.args[2].name.asCodeString()).isEqualTo("c")
    assertThat(annotationC.args[2].value).isInstanceOf(ElementValue.AnnotationValue::class.java)

    assertThat(annotationD.args).isEmpty()

    assertThat(annotationE.args).isEmpty()
  }

  @Test
  fun anonymousInitialization_hasCorrectModifiers() {
    val input =
      """
        class Test {
          {
            print('init');
          }
          static {
            print('more init');
          }
        }
        """
    val classDecl = TranslateHelpers.parseAndFindFirstNodeOfType<ClassDeclaration>(input)

    assertNotNull(classDecl)
    assertThat(classDecl.methodDeclarations).hasSize(2)

    val normalInitializer = classDecl.methodDeclarations.first()
    assertThat(normalInitializer.modifiers).isEmpty()

    val staticInitializer = classDecl.methodDeclarations.last()
    assertThat(staticInitializer.modifiers).hasSize(1)
    assertThat((staticInitializer.modifiers.first() as? KeywordModifier)?.keyword)
      .isEqualTo(KeywordModifier.Keyword.STATIC)
  }
}
