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

package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.NodeWithSourceLocation
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.expression.Expression

/**
 * Abstract base class for statements.
 *
 * @param loc the location in the source file
 */
sealed class Statement(loc: SourceLocation) : NodeWithSourceLocation(loc)

/**
 * A `continue` statement.
 *
 * @param loc the location in the source file
 */
class ContinueStatement(loc: SourceLocation) : Statement(loc) {
  /** Returns empty list of children because this is a leaf node. */
  override fun getChildren(): List<Node> = emptyList()
}

/**
 * A `break` statement.
 *
 * @param loc the location in the source file
 */
class BreakStatement(loc: SourceLocation) : Statement(loc) {
  /** Returns empty list of children because this is a leaf node. */
  override fun getChildren(): List<Node> = emptyList()
}

/**
 * A `System.runAs` statement.
 *
 * I couldn't find any examples of `System.runAs` with multiple user contexts, but the parse grammar
 * allows for an expression list and so do we.
 *
 * @property contexts the user contexts in which to run the body
 * @property body the statement to execute
 * @param loc the location in the source file
 */
class RunAsStatement(val contexts: List<Expression>, val body: Statement, loc: SourceLocation) :
  Statement(loc) {
  override fun getChildren(): List<Node> = contexts + listOf(body)
}
