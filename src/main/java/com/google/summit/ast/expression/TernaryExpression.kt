package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * The ternary conditional expression.
 *
 * See:
 * [Expression Operators](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/langCon_apex_expressions_operators_understanding.htm)
 *
 * @property condition the conditional expression
 * @property thenValue evaluated result if the condition is true
 * @property elseValue evaluated result if the condition is false
 * @param loc the location in the source file
 */
class TernaryExpression(
  val condition: Expression,
  val thenValue: Expression,
  val elseValue: Expression,
  loc: SourceLocation
) : Expression(loc) {

  override fun getChildren(): List<Node> = listOf(condition, thenValue, elseValue)
}
