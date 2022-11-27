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
 * A binary (two-operand) expression.
 *
 * This excludes assignments.
 *
 * @property left the first operand
 * @property op the operation applied
 * @property right the second operand
 * @param loc the location in the source file
 */
class BinaryExpression(
  val left: Expression,
  val op: Operator,
  val right: Expression,
  loc: SourceLocation
) : Expression(loc) {

  /**
   * This is the specific operation applied.
   *
   * See:
   * [Expression Operators](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/langCon_apex_expressions_operators_understanding.htm)
   */
  enum class Operator {
    ADDITION,
    SUBTRACTION,
    MULTIPLICATION,
    DIVISION,
    GREATER_THAN_OR_EQUAL,
    GREATER_THAN,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    EQUAL,
    NOT_EQUAL,
    ALTERNATIVE_NOT_EQUAL,
    EXACTLY_EQUAL,
    EXACTLY_NOT_EQUAL,
    INSTANCEOF,
    LEFT_SHIFT,
    RIGHT_SHIFT_SIGNED,
    RIGHT_SHIFT_UNSIGNED,
    BITWISE_AND,
    BITWISE_OR,
    BITWISE_XOR,
    LOGICAL_AND,
    LOGICAL_OR,
  }

  /** Constructs an expression from a string representation of the operator. */
  constructor(
    left: Expression,
    opString: String,
    right: Expression,
    loc: SourceLocation
  ) : this(left, toOperator(opString), right, loc)

  override fun getChildren(): List<Node> = listOf(left, right)

  companion object BinaryExpression {
    /**
     * Returns an [Operator] from its Apex code string.
     *
     * @param str the case-insensitive operator string
     * @return the corresponding [Operator]
     * @throws IllegalArgumentException if [str] is not a valid operator
     */
    fun toOperator(str: String): Operator =
      when (str.lowercase()) {
        "+" -> Operator.ADDITION
        "-" -> Operator.SUBTRACTION
        "*" -> Operator.MULTIPLICATION
        "/" -> Operator.DIVISION
        ">" -> Operator.GREATER_THAN
        ">=" -> Operator.GREATER_THAN_OR_EQUAL
        "<" -> Operator.LESS_THAN
        "<=" -> Operator.LESS_THAN_OR_EQUAL
        "==" -> Operator.EQUAL
        "!=" -> Operator.NOT_EQUAL
        "<>" -> Operator.ALTERNATIVE_NOT_EQUAL
        "===" -> Operator.EXACTLY_EQUAL
        "!==" -> Operator.EXACTLY_NOT_EQUAL
        "instanceof" -> Operator.INSTANCEOF
        "<<" -> Operator.LEFT_SHIFT
        ">>" -> Operator.RIGHT_SHIFT_SIGNED
        ">>>" -> Operator.RIGHT_SHIFT_UNSIGNED
        "&" -> Operator.BITWISE_AND
        "|" -> Operator.BITWISE_OR
        "^" -> Operator.BITWISE_XOR
        "&&" -> Operator.LOGICAL_AND
        "||" -> Operator.LOGICAL_OR
        else -> throw IllegalArgumentException("Unknown operator '$str'")
      }

    /**
     * Returns an Apex code string representation of an [Operator].
     *
     * Both `!=` and `<>` are [Operator.NOT_EQUAL]; the former is returned.
     *
     * @param op the [Operator]
     * @return the corresponding string
     */
    fun toString(op: Operator): String =
      when (op) {
        Operator.ADDITION -> "+"
        Operator.SUBTRACTION -> "-"
        Operator.MULTIPLICATION -> "*"
        Operator.DIVISION -> "/"
        Operator.GREATER_THAN -> ">"
        Operator.GREATER_THAN_OR_EQUAL -> ">="
        Operator.LESS_THAN -> "<"
        Operator.LESS_THAN_OR_EQUAL -> "<="
        Operator.EQUAL -> "=="
        Operator.NOT_EQUAL -> "!="
        Operator.ALTERNATIVE_NOT_EQUAL -> "<>"
        Operator.EXACTLY_EQUAL -> "==="
        Operator.EXACTLY_NOT_EQUAL -> "!=="
        Operator.INSTANCEOF -> "instanceof"
        Operator.LEFT_SHIFT -> "<<"
        Operator.RIGHT_SHIFT_SIGNED -> ">>"
        Operator.RIGHT_SHIFT_UNSIGNED -> ">>>"
        Operator.BITWISE_AND -> "&"
        Operator.BITWISE_OR -> "|"
        Operator.BITWISE_XOR -> "^"
        Operator.LOGICAL_AND -> "&&"
        Operator.LOGICAL_OR -> "||"
      }
  }
}
