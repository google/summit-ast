package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.declaration.VariableDeclaration

/**
 * A `try` statement.
 *
 * @property body the statement to `try`
 * @property catchBlocks the `catch` blocks
 * @property finallyBlocks the optional `finally` block
 * @param loc the location in the source file
 */
class TryStatement(
  val catchBlocks: List<CatchBlock>,
  val body: Statement,
  val finallyBlock: Statement?,
  loc: SourceLocation
) : Statement(loc) {
  override fun getChildren(): List<Node> = catchBlocks + listOfNotNull(body, finallyBlock)

  /**
   * A `catch` block.
   *
   * @property exceptionVariable the exception variable declaration
   * @property body the statement to execute when caught
   * @param loc the location in the source file
   */
  class CatchBlock(
    val exceptionVariable: VariableDeclaration,
    val body: Statement,
    loc: SourceLocation
  ) : Statement(loc) {
    override fun getChildren(): List<Node> = listOf(exceptionVariable, body)
  }
}
