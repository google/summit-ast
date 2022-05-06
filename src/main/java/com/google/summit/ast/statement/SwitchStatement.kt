package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.declaration.VariableDeclaration
import com.google.summit.ast.expression.Expression

/**
 * A `switch` statement.
 *
 * @property condition the expression to match
 * @property whenClauses a list of match conditions
 * @param loc the location in the source file
 */
class SwitchStatement(val condition: Expression, val whenClauses: List<When>, loc: SourceLocation) :
  Statement(loc) {
  override fun getChildren(): List<Node> = listOf(condition) + whenClauses

  /**
   * An abstract class for `when` clauses.
   *
   * @property statement to execute when matched
   */
  sealed class When(val statement: Statement) : Node() {
    /** Returns an unknown location, but there are locations in the subtree. */
    override fun getSourceLocation(): SourceLocation = SourceLocation.UNKNOWN
  }

  /**
   * A `when` clause that is matched via one of the listed values.
   *
   * @property values to compare against the switch expression
   * @property statement to execute when matched
   */
  class WhenValue(val values: List<Expression>, statement: Statement) : When(statement) {
    override fun getChildren(): List<Node> = values + listOf(statement)
  }

  /**
   * A `when` clause that matches on the type of the switch expression.
   *
   * The expression is cast to a new variable of the type.
   *
   * @property type the type to match
   * @property variableDeclaration for the downcast switch condition
   * @property statement to execute when matched
   */
  class WhenType(
    val type: TypeRef,
    val variableDeclaration: VariableDeclaration,
    statement: Statement
  ) : When(statement) {
    override fun getChildren(): List<Node> = listOf(type, variableDeclaration, statement)
  }

  /**
   * The `when else` case.
   *
   * @property statement to execute when matched
   */
  class WhenElse(statement: Statement) : When(statement) {
    override fun getChildren(): List<Node> = listOf(statement)
  }
}
