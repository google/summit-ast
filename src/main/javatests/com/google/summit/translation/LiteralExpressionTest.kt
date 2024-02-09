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
import com.google.summit.ast.expression.LiteralExpression
import com.google.summit.testing.TranslateHelpers
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.math.BigDecimal

@RunWith(JUnit4::class)
class LiteralExpressionTest {

  /** Concatenates the string in a field initializer context and returns the code string. */
  private fun createCompilationUnitCodeUsingExpression(expression: String): String =
    """
        class Test {
          Object x = $expression;
        }
      """

  @Test
  fun null_translation_isNullLiteral() {
    val code = createCompilationUnitCodeUsingExpression("null")
    val node = TranslateHelpers.parseAndFindFirstNodeOfType<LiteralExpression.NullVal>(code)

    assertThat(node).isNotNull()
  }

  @Test
  fun true_translation_isBooleanLiteralWithValue() {
    val code = createCompilationUnitCodeUsingExpression("true")
    val node = TranslateHelpers.parseAndFindFirstNodeOfType<LiteralExpression.BooleanVal>(code)

    assertNotNull(node)
    assertThat(node.value).isTrue()
  }

  @Test
  fun false_translation_isBooleanLiteralWithValue() {
    val code = createCompilationUnitCodeUsingExpression("false")
    val node = TranslateHelpers.parseAndFindFirstNodeOfType<LiteralExpression.BooleanVal>(code)

    assertNotNull(node)
    assertThat(node.value).isFalse()
  }

  @Test
  fun integer_translation_isIntegerLiteralWithValue() {
    val code = createCompilationUnitCodeUsingExpression("1234")
    val node = TranslateHelpers.parseAndFindFirstNodeOfType<LiteralExpression.IntegerVal>(code)

    assertNotNull(node)
    assertThat(node.value).isEqualTo(1234)
  }

  @Test
  fun long_translation_isLongLiteralWithValue() {
    val code = createCompilationUnitCodeUsingExpression("1234L")
    val node = TranslateHelpers.parseAndFindFirstNodeOfType<LiteralExpression.LongVal>(code)

    assertNotNull(node)
    assertThat(node.value).isEqualTo(1234)
  }

  @Test
  fun number_translation_isDecimalLiteralWithValue() {
    val code = createCompilationUnitCodeUsingExpression("0.1")
    val node = TranslateHelpers.parseAndFindFirstNodeOfType<LiteralExpression.DecimalVal>(code)

    assertNotNull(node)
    assertThat(node.value).isEqualTo(BigDecimal("0.1"))
  }

  @Test
  fun number_translation_isDoubleLiteralWithValue() {
    val code = createCompilationUnitCodeUsingExpression("100.0D")
    val node = TranslateHelpers.parseAndFindFirstNodeOfType<LiteralExpression.DoubleVal>(code)

    assertNotNull(node)
    assertThat(node.value).isEqualTo(100.0)
  }

  @Test
  fun string_translation_isStringLiteralWithValue() {
    val code = createCompilationUnitCodeUsingExpression("'hello'")
    val node = TranslateHelpers.parseAndFindFirstNodeOfType<LiteralExpression.StringVal>(code)

    assertNotNull(node)
    assertWithMessage("The value should be the string without quotes")
      .that(node.value)
      .isEqualTo("hello")
  }

  @Test
  fun largeInteger_throws_exceptions() {
    val code = createCompilationUnitCodeUsingExpression("999999999999999")

    assertFailsWith<Translate.TranslationException>(
      "Translation failed on 999999999999999 because Literal '999999999999999' format is incorrect",
      { TranslateHelpers.parseAndTranslateWithExceptions(code) }
    )
  }
}
