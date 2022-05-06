package com.google.summit.ast.expression

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * A variable (or parameter) reference.
 *
 * @property id the identifier
 * @param loc the location in the source file
 */
class VariableExpression(val id: Identifier, loc: SourceLocation) : Expression(loc) {
  override fun getChildren(): List<Node> = listOf(id)
}
