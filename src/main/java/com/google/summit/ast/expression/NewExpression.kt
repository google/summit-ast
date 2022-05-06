package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.initializer.Initializer

/**
 * A `new` expression.
 *
 * @property initializer of the created object
 * @param loc the location in the source file
 */
class NewExpression(val initializer: Initializer, loc: SourceLocation) : Expression(loc) {
  override fun getChildren(): List<Node> = listOf(initializer)
}
