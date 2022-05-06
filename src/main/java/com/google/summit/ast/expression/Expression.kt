package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.NodeWithSourceLocation
import com.google.summit.ast.SourceLocation

/**
 * An expression.
 *
 * @param loc the location in the source file
 */
sealed class Expression(loc: SourceLocation) : NodeWithSourceLocation(loc)

/**
 * A reference to the `super` class.
 *
 * @param loc the location in the source file
 */
class SuperExpression(loc: SourceLocation) : Expression(loc) {
  /** Returns empty list of children because this is a leaf node. */
  override fun getChildren(): List<Node> = emptyList()
}

/**
 * A reference to the `this` object.
 *
 * @param loc the location in the source file
 */
class ThisExpression(loc: SourceLocation) : Expression(loc) {
  /** Returns empty list of children because this is a leaf node. */
  override fun getChildren(): List<Node> = emptyList()
}
