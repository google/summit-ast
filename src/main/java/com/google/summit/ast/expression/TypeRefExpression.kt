package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef

/**
 * A type expression.
 *
 * @property typeRef the referenced type
 * @param loc the location in the source file
 */
class TypeRefExpression(val typeRef: TypeRef, loc: SourceLocation) : Expression(loc) {
  override fun getChildren(): List<Node> = listOf(typeRef)
}
