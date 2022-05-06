package com.google.summit.ast.initializer

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.expression.Expression

/**
 * A object initializer via a constructor call.
 *
 * @property args the constructor call arguments
 * @property type of the initialized object
 * @param loc the location in the source file
 */
class ConstructorInitializer(val args: List<Expression>, type: TypeRef, loc: SourceLocation) :
  Initializer(type, loc) {
  override fun getChildren(): List<Node> = args + listOf(type)
}
