package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.statement.CompoundStatement

/**
 * A declaration for a method.
 *
 * Note: initialization blocks (see "Using Initialization Code" in
 * [https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_static.htm])
 * in Apex code are effectively methods that do not have a name. Instead of having an empty [id] for
 * these methods, we set their id to `_init`.
 *
 * @param id the name of the method
 * @property returnType the static type of the returned value
 * @property parameterDeclarations a list of the static types of any formal parameters
 * @property body the method implementation, if defined
 * @param loc the source location in the source file
 */
class MethodDeclaration(
  id: Identifier,
  val returnType: TypeRef,
  val parameterDeclarations: List<ParameterDeclaration>,
  val body: CompoundStatement?,
  loc: SourceLocation
) : Declaration(id, loc) {
  override fun getChildren(): List<Node> =
    modifiers + listOfNotNull(id, returnType, body) + parameterDeclarations

  /**
   * Returns whether this method is an anonymous initialization block.
   *
   * See "Using Initialization Code" in
   * [https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_static.htm])
   */
  fun isAnonymousInitializationCode(): Boolean = id.string == ANONYMOUS_INITIALIZER_NAME

  companion object {
    /** This name is used for the unnamed class initializer blocks. */
    const val ANONYMOUS_INITIALIZER_NAME = "_init"
  }
}
