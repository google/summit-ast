package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * An assignment expression.
 *
 * @property target the assignment target
 * @property source the value being assigned
 * @property preOperation an optional binary operator to first apply
 * @param loc the location in the source file
 */
class AssignExpression(
  val target: Expression,
  val source: Expression,
  val preOperation: BinaryExpression.Operator?,
  loc: SourceLocation
) : Expression(loc) {

  override fun getChildren(): List<Node> = listOf(target, source)
}
