package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * An array access expression.
 *
 * @property array the array expression
 * @property index the index expression
 * @param loc the location in the source file
 */
class ArrayExpression(val array: Expression, val index: Expression, loc: SourceLocation) :
  Expression(loc) {

  override fun getChildren(): List<Node> = listOf(array, index)
}
