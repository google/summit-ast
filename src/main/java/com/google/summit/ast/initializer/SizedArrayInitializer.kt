package com.google.summit.ast.initializer

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.expression.Expression

/**
 * An object initializer for sized arrays.
 *
 * @property size of the array
 * @property type of the initialized object
 * @param loc the location in the source file
 */
class SizedArrayInitializer(val size: Expression, type: TypeRef, loc: SourceLocation) :
  Initializer(type, loc) {
  override fun getChildren(): List<Node> = listOf(size, type)
}
