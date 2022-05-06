package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * An SOQL or SOSL expression.
 *
 * @param loc the location in the source file
 */
class SoqlOrSoslExpression(loc: SourceLocation) : Expression(loc) {
  // TODO(b/216117963): Translate SOQL syntax.

  /** Returns empty list of children because this is a leaf node. */
  override fun getChildren(): List<Node> = emptyList()
}
