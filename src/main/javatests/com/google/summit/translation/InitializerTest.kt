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
import com.google.summit.ast.expression.LiteralExpression
import com.google.summit.ast.expression.NewExpression
import com.google.summit.ast.initializer.ConstructorInitializer
import com.google.summit.ast.initializer.MapInitializer
import com.google.summit.ast.initializer.SizedArrayInitializer
import com.google.summit.ast.initializer.ValuesInitializer
import com.google.summit.testing.TranslateHelpers
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InitializerTest {

  /** Concatenates the expression as a field initializer and returns the [NewExpression]. */
  private fun parseNewExpressionInCode(expression: String): NewExpression? {
    return TranslateHelpers.parseAndFindFirstNodeOfType<NewExpression>(
      """
        class Test {
          Object x = $expression;
        }
        """
    )
  }

  @Test
  fun constructor_translation_isConstructorInitializer() {
    val node = parseNewExpressionInCode("new String('hello world')")

    assertNotNull(node)
    val ctorInitializer = node.initializer as? ConstructorInitializer
    assertNotNull(ctorInitializer)
    assertThat(ctorInitializer.type.asCodeString()).isEqualTo("String")
    assertThat(ctorInitializer.args).hasSize(1)
  }

  @Test
  fun emptyListInitializer_has_noValues() {
    val node = parseNewExpressionInCode("new List<String>{ }")

    assertNotNull(node)
    val valuesInitializer = node.initializer as? ValuesInitializer
    assertNotNull(valuesInitializer)
    assertThat(valuesInitializer.values).isEmpty()
  }

  @Test
  fun emptyMapInitializer_has_noValues() {
    val node = parseNewExpressionInCode("new Map<String, String>{ }")

    assertNotNull(node)
    val valuesInitializer = node.initializer as? ValuesInitializer
    assertNotNull(valuesInitializer)
    assertThat(valuesInitializer.values).isEmpty()
  }

  @Test
  fun mapInitializer_has_values() {
    val node = parseNewExpressionInCode("new Map<String, String>{ 'a' => 'b', 'c' => 'd' }")

    assertNotNull(node)
    val mapInitializer = node.initializer as? MapInitializer
    assertNotNull(mapInitializer)
    assertThat(mapInitializer.pairs).hasSize(2)
    val firstKeyValuePair = mapInitializer.pairs.first()
    assertThat(firstKeyValuePair.first).isInstanceOf(LiteralExpression.StringVal::class.java)
    assertThat(firstKeyValuePair.second).isInstanceOf(LiteralExpression.StringVal::class.java)
  }

  @Test
  fun listInitializer_has_values() {
    val node = parseNewExpressionInCode("new List<Integer>{1,2,3}")

    assertNotNull(node)
    val valuesInitializer = node.initializer as? ValuesInitializer
    assertNotNull(valuesInitializer)
    assertThat(valuesInitializer.values).hasSize(3)
  }

  @Test
  fun setInitializer_has_values() {
    val node = parseNewExpressionInCode("new Set<Integer>{1,2,3}")

    assertNotNull(node)
    val valuesInitializer = node.initializer as? ValuesInitializer
    assertNotNull(valuesInitializer)
    assertThat(valuesInitializer.values).hasSize(3)
  }

  @Test
  fun arrayValuesInitializer_has_values() {
    val node = parseNewExpressionInCode("new Integer[] {1,2,3}")

    assertNotNull(node)
    val valuesInitializer = node.initializer as? ValuesInitializer
    assertNotNull(valuesInitializer)
    assertThat(valuesInitializer.values).hasSize(3)
  }

  @Test
  fun arraySizeInitializer_has_size() {
    val node = parseNewExpressionInCode("new Integer[5]")

    assertNotNull(node)
    val arrayInitializer = node.initializer as? SizedArrayInitializer
    assertNotNull(arrayInitializer)
    assertThat(arrayInitializer.size).isInstanceOf(LiteralExpression.IntegerVal::class.java)
  }
}
