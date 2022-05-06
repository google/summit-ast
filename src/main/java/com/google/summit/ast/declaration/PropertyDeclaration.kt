package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef

/**
 * A declaration for a property.
 *
 * @param id the name of the field
 * @property type a reference to the type of the field
 * @property getter an optional getter method implementation
 * @property getter an optional setter method implementation
 * @param sourceLocation the location in the source file
 */
class PropertyDeclaration(
  id: Identifier,
  val type: TypeRef,
  val getter: MethodDeclaration?,
  val setter: MethodDeclaration?,
  loc: SourceLocation
) : Declaration(id, loc) {

  override fun getChildren(): List<Node> = modifiers + listOfNotNull(id, type, getter, setter)
}
