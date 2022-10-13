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
 * A SOQL or SOSL expression.
 *
 * @property query the raw string
 * @property bindings the bound expressions
 * @param loc the location in the source file
 */
class SoqlOrSoslExpression(
  val query: String,
  val bindings: List<SoqlOrSoslBinding>,
  loc: SourceLocation,
) : Expression(loc) {
  // TODO(b/216117963): Translate SOQL syntax (beyond bound expressions).

  /** Returns list of children. */
  override fun getChildren(): List<Node> = bindings
}

/**
 * A SOQL or SOSL expression binding.
 *
 * @property expr the bound expression
 */
class SoqlOrSoslBinding(val expr: Expression) : Node() {

  /** Returns list of children. */
  override fun getChildren(): List<Node> = listOf(expr)

  /** Returns the [SourceLocation] of the bound expression. */
  override fun getSourceLocation(): SourceLocation = expr.getSourceLocation()
}
