package com.google.summit.ast.expression

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * A field access expression.
 *
 * @property obj the object being accessed
 * @property field name being accessed
 * @property isSafe whether this is a null-safe access
 * @param loc the location in the source file
 */
class FieldExpression(
  val obj: Expression,
  val field: Identifier,
  val isSafe: Boolean,
  loc: SourceLocation
) : Expression(loc) {

  override fun getChildren(): List<Node> = listOf(obj, field)
}
