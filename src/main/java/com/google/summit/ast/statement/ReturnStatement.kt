package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.expression.Expression

/**
 * A `return` statement (with or without value).
 *
 * @property value the returned value, if any
 * @param loc the location in the source file
 */
class ReturnStatement(val value: Expression?, loc: SourceLocation) : Statement(loc) {
  override fun getChildren(): List<Node> = listOfNotNull(value)
}
