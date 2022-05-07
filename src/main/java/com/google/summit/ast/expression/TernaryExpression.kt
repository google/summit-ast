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

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * The ternary conditional expression.
 *
 * See:
 * [Expression Operators](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/langCon_apex_expressions_operators_understanding.htm)
 *
 * @property condition the conditional expression
 * @property thenValue evaluated result if the condition is true
 * @property elseValue evaluated result if the condition is false
 * @param loc the location in the source file
 */
class TernaryExpression(
  val condition: Expression,
  val thenValue: Expression,
  val elseValue: Expression,
  loc: SourceLocation
) : Expression(loc) {

  override fun getChildren(): List<Node> = listOf(condition, thenValue, elseValue)
}
