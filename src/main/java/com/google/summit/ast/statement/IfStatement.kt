package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.expression.Expression

/**
 * An if/else statement.
 *
 * @property condition the branch expression
 * @property thenStatement the statement executed when condition is true
 * @property elseStatement the optional statement executed when condition is false
 * @param loc the location in the source file
 */
class IfStatement(
  val condition: Expression,
  val thenStatement: Statement,
  val elseStatement: Statement?,
  loc: SourceLocation
) : Statement(loc) {
  override fun getChildren(): List<Node> = listOfNotNull(condition, thenStatement, elseStatement)
}
