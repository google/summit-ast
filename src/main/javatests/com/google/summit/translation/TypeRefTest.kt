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
import com.google.summit.ast.TypeRef
import com.google.summit.testing.TranslateHelpers
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeRefTest {

  @Test
  fun typeRef_supports_multipleComponents() {
    val input = "class Test extends A.B.C.D { }"

    val typeRefNode = TranslateHelpers.parseAndFindFirstNodeOfType<TypeRef>(input)

    assertNotNull(typeRefNode)
    assertThat(typeRefNode.components).hasSize(4)
  }

  @Test
  fun typeRef_supports_genericArguments() {
    val input = "class Test extends A.B<C, D.E<F>> { }"

    val outerTypeRefNode = TranslateHelpers.parseAndFindFirstNodeOfType<TypeRef>(input)

    assertNotNull(outerTypeRefNode)
    assertThat(outerTypeRefNode.components).hasSize(2) // A, B
    assertThat(outerTypeRefNode.components[1].args).hasSize(2) // C, D.E<F>
    assertThat(outerTypeRefNode.components[1].args[1].components).hasSize(2) // D, E
    assertThat(outerTypeRefNode.components[1].args[1].components[1].args).hasSize(1) // F
  }

  @Test
  fun typeRef_supports_arrayNesting() {
    val input = "class Test extends A[ ][] { }"

    val typeRefNode = TranslateHelpers.parseAndFindFirstNodeOfType<TypeRef>(input)

    assertNotNull(typeRefNode)
    assertThat(typeRefNode.arrayNesting).isEqualTo(2)
  }

  @Test
  fun typeRef_translatedFrom_parseTreeTerminal() {
    // Here, "Map" is special because it is a token / terminal symbol
    val input = "class Test extends Map<String> { }"

    val typeRefNode = TranslateHelpers.parseAndFindFirstNodeOfType<TypeRef>(input)

    assertNotNull(typeRefNode)
    assertThat(typeRefNode.asCodeString()).isEqualTo("Map<String>")
  }
}
