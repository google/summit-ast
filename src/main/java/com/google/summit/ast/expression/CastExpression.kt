package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef

/**
 * A cast expression.
 *
 * @property value the instance being cast
 * @property type the resulting type
 * @param loc the location in the source file
 */
class CastExpression(val value: Expression, val type: TypeRef, loc: SourceLocation) :
  Expression(loc) {

  override fun getChildren(): List<Node> = listOf(value, type)
}
