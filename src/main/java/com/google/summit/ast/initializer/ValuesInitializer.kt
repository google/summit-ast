package com.google.summit.ast.initializer

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.expression.Expression

/**
 * An object initializer for lists, sets, or arrays via a list of values.
 *
 * An empty set of values may also be used to initialize maps.
 *
 * @property values in the initialized collection
 * @property type of the initialized object
 * @param loc the location in the source file
 */
class ValuesInitializer(val values: List<Expression>, type: TypeRef, loc: SourceLocation) :
  Initializer(type, loc) {
  override fun getChildren(): List<Node> = values + listOf(type)
}
