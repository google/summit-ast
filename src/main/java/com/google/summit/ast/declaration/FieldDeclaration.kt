package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.expression.Expression

/**
 * A declaration for a field (a.k.a. class member).
 *
 * @param id the name of the field
 * @property type a reference to the type of the field
 * @property initializer an optional initializer expression
 * @param sourceLocation the location in the source file
 */
class FieldDeclaration(
  id: Identifier,
  val type: TypeRef,
  val initializer: Expression?,
  loc: SourceLocation
) : Declaration(id, loc) {

  override fun getChildren(): List<Node> = modifiers + listOfNotNull(id, type, initializer)
}
