package com.google.summit.ast.expression

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * A method call expression.
 *
 * There are several special cases. An identifier:
 * * `this` indicates constructor chaining. See
 * [https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_constructors.htm]
 * * `super` indicates a base class constructor call. See
 * [https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_example.htm]
 *
 * @property receiver the receiver object
 * @property id the name of the method
 * @property isSafe whether this is a null-safe access
 * @param loc the location in the source file
 */
class CallExpression(
  val receiver: Expression?,
  val id: Identifier,
  val args: List<Expression>,
  val isSafe: Boolean = false,
  loc: SourceLocation
) : Expression(loc) {
  override fun getChildren(): List<Node> = listOfNotNull(receiver, id) + args
}
