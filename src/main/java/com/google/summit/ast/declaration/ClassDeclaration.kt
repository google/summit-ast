package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef

/**
 * A declaration for a class symbol.
 *
 * @param id the unqualified name of the class
 * @param extendsType an optional type reference to a super class
 * @param implementsTypes a list of type references to implemented interfaces
 * @param bodyDeclarations a list of the declarations inside the class body
 * @param loc the location in the source file
 */
class ClassDeclaration(
  id: Identifier,
  val extendsType: TypeRef?,
  val implementsTypes: List<TypeRef>,
  bodyDeclarations: List<Declaration>,
  loc: SourceLocation
) : TypeDeclaration(id, loc) {

  /** The subset of body declarations that declare an inner type (class, enum, or interface). */
  val innerTypeDeclarations = bodyDeclarations.filterIsInstance<TypeDeclaration>()
  /** The field declarations include both static and instance members. */
  val fieldDeclarations = bodyDeclarations.filterIsInstance<FieldDeclaration>()
  /** The method declarations include both static and instance methods. */
  val methodDeclarations = bodyDeclarations.filterIsInstance<MethodDeclaration>()
  /** The property declarations include both static and instance properties. */
  val propertyDeclarations = bodyDeclarations.filterIsInstance<PropertyDeclaration>()

  override fun getChildren(): List<Node> =
    modifiers +
      listOfNotNull(id, extendsType) +
      implementsTypes +
      innerTypeDeclarations +
      fieldDeclarations +
      methodDeclarations +
      propertyDeclarations
}
