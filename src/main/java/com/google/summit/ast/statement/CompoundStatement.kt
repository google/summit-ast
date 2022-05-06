package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * A compound statement.
 *
 * This is a block of multiple statements, combined into the syntactic role of a single statement.
 *
 * In the most typical case (a block enclosed by curly braces), the compound statement is also a
 * scoping boundary: variables declared inside are not visible outside of the subtree. In other
 * circumstances (such as "lowering" syntax or transforming the AST), a scoping boundary may not be
 * appropriate.
 *
 * @property statements the list of statements
 * @property scoping whether this is a scoping boundary
 * @param loc the location in the source file
 */
class CompoundStatement(
  val statements: List<Statement>,
  val scoping: Scoping,
  loc: SourceLocation
) : Statement(loc) {
  /** This determines whether symbols declared inside are visible externally. */
  enum class Scoping {
    SCOPE_BOUNDARY,
    SCOPE_TRANSPARENT,
  }

  override fun getChildren(): List<Node> = statements
}
