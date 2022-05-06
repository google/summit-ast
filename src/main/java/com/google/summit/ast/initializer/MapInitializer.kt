package com.google.summit.ast.initializer

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.expression.Expression

/**
 * An object initializer for maps via a list of key-value pairs.
 *
 * @property pairs the list of key-value expressions
 * @property type of the initialized object
 * @param loc the location in the source file
 */
class MapInitializer(
  val pairs: List<Pair<Expression, Expression>>,
  type: TypeRef,
  loc: SourceLocation
) : Initializer(type, loc) {
  override fun getChildren(): List<Node> = pairs.flatMap { it.toList() } + listOf(type)
}
