package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.expression.Expression

/**
 * A `throw` statement.
 *
 * @property exception the object to throw
 * @param loc the location in the source file
 */
class ThrowStatement(val exception: Expression, loc: SourceLocation) : Statement(loc) {
  override fun getChildren(): List<Node> = listOf(exception)
}
