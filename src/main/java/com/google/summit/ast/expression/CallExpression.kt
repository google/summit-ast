/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
