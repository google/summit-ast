package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.expression.Expression

/**
 * An expression in a statement context.
 *
 * @property expression the expression
 * @param loc the location in the source file
 */
class ExpressionStatement(val expression: Expression, loc: SourceLocation) : Statement(loc) {
  override fun getChildren(): List<Node> = listOf(expression)
}
