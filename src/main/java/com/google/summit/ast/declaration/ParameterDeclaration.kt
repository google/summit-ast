package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef

/**
 * A formal parameter for a method.
 *
 * @property name the parameter name
 * @property type the static type of the parameter
 * @param loc the location in the source file
 */
class ParameterDeclaration(val name: Identifier, val type: TypeRef, loc: SourceLocation) :
  Declaration(name, loc) {

  override fun getChildren(): List<Node> = modifiers + listOf(type, name)
}
