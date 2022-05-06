package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.declaration.VariableDeclaration

/**
 * A variable declaration statement.
 *
 * @property variableDeclarations the declared variables
 * @param loc the location in the source file
 */
class VariableDeclarationStatement(
  val variableDeclarations: List<VariableDeclaration>,
  loc: SourceLocation
) : Statement(loc) {

  override fun getChildren(): List<Node> = variableDeclarations
}
