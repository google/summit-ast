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
import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.Node
import com.google.summit.ast.expression.ArrayExpression
import com.google.summit.ast.expression.AssignExpression
import com.google.summit.ast.expression.BinaryExpression
import com.google.summit.ast.expression.CallExpression
import com.google.summit.ast.expression.CastExpression
import com.google.summit.ast.expression.Expression
import com.google.summit.ast.expression.FieldExpression
import com.google.summit.ast.expression.NewExpression
import com.google.summit.ast.expression.SoqlExpression
import com.google.summit.ast.expression.SoslExpression
import com.google.summit.ast.expression.SuperExpression
import com.google.summit.ast.expression.TernaryExpression
import com.google.summit.ast.expression.ThisExpression
import com.google.summit.ast.expression.TypeRefExpression
import com.google.summit.ast.expression.UnaryExpression
import com.google.summit.ast.expression.UntranslatedExpression
import com.google.summit.ast.expression.VariableExpression
import com.google.summit.ast.traversal.DfsWalker
import com.google.summit.testing.TranslateHelpers
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExpressionTest {

  /** Counts the number of untranslated expression nodes in the AST. */
  private fun countUntranslatedExpressions(node: Node): Long {
    return DfsWalker(node, DfsWalker.Ordering.PRE_ORDER)
      .stream()
      .filter { it is UntranslatedExpression }
      .count()
  }

  /** Concatenates the string in a field initializer context and returns the AST. */
  private fun parseApexExpressionInCode(expression: String): CompilationUnit {
    return TranslateHelpers.parseAndTranslate(
      """
        class Test {
          Object x = $expression;
        }
        """
    )
  }

  @Test
  fun thisPrimary_translation_isLeafNode() {
    val root = parseApexExpressionInCode("this")
    val node = TranslateHelpers.findFirstNodeOfType<ThisExpression>(root)

    assertThat(node).isNotNull()
    assertWithMessage("Node should have no children").that(node?.getChildren()).isEmpty()
  }

  @Test
  fun superPrimary_translation_isLeafNode() {
    val root = parseApexExpressionInCode("super")
    val node = TranslateHelpers.findFirstNodeOfType<SuperExpression>(root)

    assertThat(node).isNotNull()
    assertWithMessage("Node should have no children").that(node?.getChildren()).isEmpty()
  }

  @Test
  fun typeRefPrimary_translation_hasOneChild() {
    val root = parseApexExpressionInCode("Object.class")
    val node = TranslateHelpers.findFirstNodeOfType<TypeRefExpression>(root)

    assertThat(node).isNotNull()
    assertWithMessage("Node should have one child").that(node?.getChildren()).hasSize(1)
  }

  @Test
  fun idPrimary_translation_hasCorrectIdentifier() {
    val root = parseApexExpressionInCode("id")
    val node = TranslateHelpers.findFirstNodeOfType<VariableExpression>(root)

    assertThat(node).isNotNull()
    assertThat(node?.id?.asCodeString()).isEqualTo("id")
  }

  @Test
  fun soqlPrimary_translation_hasBoundExpressions() {
    val root = parseApexExpressionInCode("[SELECT Id FROM Contact WHERE Value > :Threshold]")
    val node = TranslateHelpers.findFirstNodeOfType<SoqlExpression>(root)

    assertThat(node).isNotNull()
    assertWithMessage("Node should have one child").that(node?.getChildren()).hasSize(1)
  }

  @Test
  fun soslPrimary_translation_hasBoundExpressions() {
    val root = parseApexExpressionInCode("[FIND :search IN ALL FIELDS RETURNING Account(Name)]")
    val node = TranslateHelpers.findFirstNodeOfType<SoslExpression>(root)

    assertThat(node).isNotNull()
    assertWithMessage("Node should have one child").that(node?.getChildren()).hasSize(1)
  }

  /**
   * Concatenates the operator string inside a binary expression and returns the [BinaryExpression].
   */
  private fun parseBinaryOperatorInCode(operator: String): BinaryExpression? {
    return TranslateHelpers.parseAndFindFirstNodeOfType<BinaryExpression>(
      """
        class Test {
          Object x = y $operator z;
        }
        """
    )
  }

  /** Test all binary operators and also the translation of all binary expression rules. */
  @Test
  fun allOperators_match_binaryExpressionOp() {
    for (op in BinaryExpression.Operator.values()) {
      val node = parseBinaryOperatorInCode(BinaryExpression.toString(op))

      assertNotNull(node)
      assertThat(node.op).isEqualTo(op)
      assertWithMessage("`BinaryExpression.toOperator` and `.toString` should be inverses")
        .that(BinaryExpression.toOperator(BinaryExpression.toString(op)))
        .isEqualTo(op)
    }
  }

  @Test
  fun fieldAccess_translation_isFieldExpression() {
    val root = parseApexExpressionInCode("x.y")
    val node = TranslateHelpers.findFirstNodeOfType<FieldExpression>(root)

    assertNotNull(node)
    assertWithMessage("Node should have two children").that(node.getChildren()).hasSize(2)
    assertThat(node.isSafe).isFalse()
    assertThat(node.field.asCodeString()).isEqualTo("y")
  }

  @Test
  fun safeAccess_sets_FieldExpression_isSafe() {
    val root = parseApexExpressionInCode("x?.y")
    val node = TranslateHelpers.findFirstNodeOfType<FieldExpression>(root)

    assertNotNull(node)
    assertThat(node.isSafe).isTrue()
  }

  @Test
  fun arrayAccess_translation_isArrayExpression() {
    val root = parseApexExpressionInCode("a[b]")
    val node = TranslateHelpers.findFirstNodeOfType<ArrayExpression>(root)

    assertNotNull(node)
    assertWithMessage("Node should have two children").that(node.getChildren()).hasSize(2)
  }

  @Test
  fun newClassObject_translation_isNewExpression() {
    val root = parseApexExpressionInCode("new String()")
    val node = TranslateHelpers.findFirstNodeOfType<NewExpression>(root)

    assertNotNull(node)
    assertThat(node.initializer.type.asCodeString()).isEqualTo("String")
  }

  @Test
  fun newSizedArray_translation_isNewExpression() {
    val root = parseApexExpressionInCode("new Double[5]")
    val node = TranslateHelpers.findFirstNodeOfType<NewExpression>(root)

    assertNotNull(node)
    assertThat(node.initializer.type.asCodeString()).isEqualTo("Double[]")
  }

  @Test
  fun newInitializedArray_translation_isNewExpression() {
    val root = parseApexExpressionInCode("new Double[] { 1.0, 2.0 }")
    val node = TranslateHelpers.findFirstNodeOfType<NewExpression>(root)

    assertNotNull(node)
    assertThat(node.initializer.type.asCodeString()).isEqualTo("Double[]")
  }

  @Test
  fun newInitializedList_translation_isNewExpression() {
    val root = parseApexExpressionInCode("new List<Double> { 1.0, 2.0 }")
    val node = TranslateHelpers.findFirstNodeOfType<NewExpression>(root)

    assertNotNull(node)
    assertThat(node.initializer.type.asCodeString()).isEqualTo("List<Double>")
  }

  @Test
  fun newInitializedMap_translation_isNewExpression() {
    val root = parseApexExpressionInCode("new Map<String, String>{'a' => 'b', 'c' => 'd'}")
    val node = TranslateHelpers.findFirstNodeOfType<NewExpression>(root)

    assertNotNull(node)
    assertThat(node.initializer.type.asCodeString()).isEqualTo("Map<String, String>")
  }

  /**
   * Concatenates the assignment operator string as a statement and returns the [AssignExpression].
   */
  private fun parseAssignOperatorInCode(operator: String): AssignExpression? {
    return TranslateHelpers.parseAndFindFirstNodeOfType<AssignExpression>(
      """
        class Test {
          void f() { x $operator y; }
        }
        """
    )
  }

  @Test
  fun assignExpression_produces_untranslatedNode() {
    val assignOperations =
      mapOf(
        null to "=",
        BinaryExpression.Operator.ADDITION to "+=",
        BinaryExpression.Operator.SUBTRACTION to "-=",
        BinaryExpression.Operator.MULTIPLICATION to "*=",
        BinaryExpression.Operator.DIVISION to "/=",
        BinaryExpression.Operator.MODULO to "%=",
        BinaryExpression.Operator.BITWISE_AND to "&=",
        BinaryExpression.Operator.BITWISE_OR to "|=",
        BinaryExpression.Operator.BITWISE_XOR to "^=",
        BinaryExpression.Operator.RIGHT_SHIFT_SIGNED to ">>=",
        BinaryExpression.Operator.RIGHT_SHIFT_UNSIGNED to ">>>=",
        BinaryExpression.Operator.LEFT_SHIFT to "<<=",
      )

    for ((op, assignString) in assignOperations) {
      val node = parseAssignOperatorInCode(assignString)

      assertNotNull(node)
      assertThat(node.preOperation).isEqualTo(op)
    }
  }

  @Test
  fun constructorChaining_encoded_asMethodNamedThis() {
    val root = parseApexExpressionInCode("this(x, y)")
    val node = TranslateHelpers.findFirstNodeOfType<CallExpression>(root)

    assertNotNull(node)
    assertWithMessage("Constructor chaining is translated as call to a method named `this`")
      .that(node.id.asCodeString())
      .isEqualTo("this")
    assertThat(node.isSafe).isFalse()
    assertThat(node.receiver).isNull()
    assertThat(node.args).hasSize(2)
  }

  @Test
  fun baseClassConstructor_encoded_asMethodNamedSuper() {
    val root = parseApexExpressionInCode("super(x, y)")
    val node = TranslateHelpers.findFirstNodeOfType<CallExpression>(root)

    assertNotNull(node)
    assertWithMessage("Base class construction is translated as call to a method named `super`")
      .that(node.id.asCodeString())
      .isEqualTo("super")
    assertThat(node.isSafe).isFalse()
    assertThat(node.receiver).isNull()
    assertThat(node.args).hasSize(2)
  }

  @Test
  fun implicitReceiver_is_null() {
    val root = parseApexExpressionInCode("no_receiver()")
    val node = TranslateHelpers.findFirstNodeOfType<CallExpression>(root)

    assertNotNull(node)
    assertThat(node.receiver).isNull()
    assertThat(node.id.asCodeString()).isEqualTo("no_receiver")
    assertThat(node.isSafe).isFalse()
    assertThat(node.args).hasSize(0)
  }

  @Test
  fun safeAccess_sets_isSafe_true() {
    val root = parseApexExpressionInCode("x?.method()")
    val node = TranslateHelpers.findFirstNodeOfType<CallExpression>(root)

    assertNotNull(node)
    assertThat(node.isSafe).isTrue()
    assertThat(node.receiver).isNotNull()
    assertThat(node.args).hasSize(0)
  }

  @Test
  fun unsafeAccess_sets_isSafe_false() {
    val root = parseApexExpressionInCode("x.method(123)")
    val node = TranslateHelpers.findFirstNodeOfType<CallExpression>(root)

    assertNotNull(node)
    assertThat(node.isSafe).isFalse()
    assertThat(node.receiver).isNotNull()
    assertThat(node.args).hasSize(1)
  }

  @Test
  fun cast_translated_asCastExpression() {
    val root = parseApexExpressionInCode("(String) obj")
    val node = TranslateHelpers.findFirstNodeOfType<CastExpression>(root)

    assertNotNull(node)
    assertWithMessage("Node should have two children").that(node.getChildren()).hasSize(2)
    assertThat(node.type.asCodeString()).isEqualTo("String")
  }

  @Test
  fun conditional_translatesTo_ternaryExpression() {
    val root = parseApexExpressionInCode("cond ? thenvalue : elsevalue")
    val node = TranslateHelpers.findFirstNodeOfType<TernaryExpression>(root)

    assertWithMessage("A `TernaryExpression` node should be created").that(node).isNotNull()
  }

  /** Test all unary operators and also the translation of all unary expression rules. */
  @Test
  fun allOperators_match_unaryExpressionOp() {
    for (op in UnaryExpression.Operator.values()) {
      val root = parseApexExpressionInCode(UnaryExpression.toString(op, "x"))
      val node = TranslateHelpers.findFirstNodeOfType<UnaryExpression>(root)

      assertNotNull(node)
      assertThat(node.op).isEqualTo(op)
    }
  }

  @Test
  fun subExpression_is_transparent() {
    val root = parseApexExpressionInCode("(sub)")
    val expression = TranslateHelpers.findFirstNodeOfType<Expression>(root)

    TranslateHelpers.assertFullyTranslated(root)
    assertWithMessage("The first expression should be inside the subexpression")
      .that(expression)
      .isInstanceOf(VariableExpression::class.java)
  }
}
