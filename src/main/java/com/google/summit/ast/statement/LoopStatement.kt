package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.declaration.VariableDeclaration
import com.google.summit.ast.expression.Expression

/**
 * A base class for all loop statements.
 *
 * @property body the statement being looped
 * @param loc the location in the source file
 */
sealed class LoopStatement(val body: Statement, loc: SourceLocation) : Statement(loc)

/**
 * A traditional three-part `for` loop statement.
 *
 * @property declarations the variables declared at the start
 * @property initializations the expressions initialized at the start
 * @property updates the expressions evaluated on each iteration
 * @property condition the optional loop condition
 * @param body the statement being looped
 * @param loc the location in the source file
 */
class ForLoopStatement(
  val declarations: List<VariableDeclaration>,
  val initializations: List<Expression>,
  val updates: List<Expression>,
  val condition: Expression?,
  body: Statement,
  loc: SourceLocation
) : LoopStatement(body, loc) {
  override fun getChildren(): List<Node> =
    declarations + initializations + listOfNotNull(condition, body) + updates
}

/**
 * A enhanced `for` loop statement.
 *
 * @property elementDeclaration the element iterator
 * @property collection the collection to iterate on
 * @param body the statement being looped
 * @param loc the location in the source file
 */
class EnhancedForLoopStatement(
  val elementDeclaration: VariableDeclaration,
  val collection: Expression,
  body: Statement,
  loc: SourceLocation
) : LoopStatement(body, loc) {
  override fun getChildren(): List<Node> = listOf(elementDeclaration, collection, body)
}

/**
 * A `while` loop statement.
 *
 * @property condition the loop condition
 * @param body the statement being looped
 * @param loc the location in the source file
 */
class WhileLoopStatement(val condition: Expression, body: Statement, loc: SourceLocation) :
  LoopStatement(body, loc) {
  override fun getChildren(): List<Node> = listOf(condition, body)
}

/**
 * A `do while` loop statement.
 *
 * @property condition the loop condition
 * @param body the statement being looped
 * @param loc the location in the source file
 */
class DoWhileLoopStatement(val condition: Expression, body: Statement, loc: SourceLocation) :
  LoopStatement(body, loc) {
  override fun getChildren(): List<Node> = listOf(condition, body)
}
