package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef

/**
 * A declaration for an interface symbol.
 *
 * @param id the unqualified name of the interface
 * @param extendsTypes a list of type references to super interfaces
 * @property methodDeclarations a list of method declarations in the body
 * @param loc the location in the source file
 */
class InterfaceDeclaration(
  id: Identifier,
  val extendsTypes: List<TypeRef>,
  val methodDeclarations: List<MethodDeclaration>,
  loc: SourceLocation
) : TypeDeclaration(id, loc) {

  override fun getChildren(): List<Node> =
    modifiers + listOf(id) + extendsTypes + methodDeclarations
}
