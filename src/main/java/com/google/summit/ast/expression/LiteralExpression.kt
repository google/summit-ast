package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * The outer and base class for literal expressions.
 *
 * @param loc the location in the source file
 */
sealed class LiteralExpression(loc: SourceLocation) : Expression(loc) {

  /** Returns empty list of children because this is a leaf node. */
  override fun getChildren(): List<Node> = emptyList()

  /**
   * A string literal.
   *
   * @property value the string value
   * @param loc the location in the source file
   */
  class StringVal(val value: String, loc: SourceLocation) : LiteralExpression(loc)

  /**
   * An integer literal.
   *
   * @property value the integer value
   * @param loc the location in the source file
   */
  class IntegerVal(val value: Int, loc: SourceLocation) : LiteralExpression(loc)

  /**
   * A long literal.
   *
   * @property value the long value
   * @param loc the location in the source file
   */
  class LongVal(val value: Long, loc: SourceLocation) : LiteralExpression(loc)

  /**
   * A boolean literal.
   *
   * @property value the boolean value
   * @param loc the location in the source file
   */
  class BooleanVal(val value: Boolean, loc: SourceLocation) : LiteralExpression(loc)

  /**
   * A null literal.
   *
   * @param loc the location in the source file
   */
  class NullVal(loc: SourceLocation) : LiteralExpression(loc)

  /**
   * A (floating point) number literal.
   *
   * @property value the floating point value
   * @param loc the location in the source file
   */
  class DoubleVal(val value: Double, loc: SourceLocation) : LiteralExpression(loc)
}
