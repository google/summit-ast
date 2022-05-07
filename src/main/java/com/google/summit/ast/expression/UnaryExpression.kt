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

package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * A unary operator expression.
 *
 * @property value the sole operand
 * @property op the operation applied
 * @param loc the location in the source file
 */
class UnaryExpression(val value: Expression, val op: Operator, loc: SourceLocation) :
  Expression(loc) {

  /**
   * This is the operator applied to the sole operand.
   *
   * See:
   * [Expression Operators](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/langCon_apex_expressions_operators_understanding.htm)
   */
  enum class Operator {
    // The unary + operator is parsed by the grammar but not documented by Salesforce.
    PLUS,
    NEGATION,
    PRE_INCREMENT,
    POST_INCREMENT,
    PRE_DECREMENT,
    POST_DECREMENT,
    LOGICAL_COMPLEMENT,
    // The ~ operator is parsed by the grammar but not documented by Salesforce.
    BITWISE_NOT,
  }

  override fun getChildren(): List<Node> = listOf(value)

  companion object UnaryExpression {
    /**
     * Returns an Apex code string representation of an [Operator] applied to a [value].
     *
     * @param op the [Operator]
     * @param value the operand
     * @return the corresponding string
     */
    fun toString(op: Operator, value: String): String =
      when (op) {
        Operator.PLUS -> "+$value"
        Operator.NEGATION -> "-$value"
        Operator.PRE_INCREMENT -> "++$value"
        Operator.POST_INCREMENT -> "$value++"
        Operator.PRE_DECREMENT -> "--$value"
        Operator.POST_DECREMENT -> "$value--"
        Operator.LOGICAL_COMPLEMENT -> "!$value"
        Operator.BITWISE_NOT -> "~$value"
      }
  }
}
