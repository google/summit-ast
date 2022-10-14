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
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.declaration.VariableDeclarationGroup
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
 * @property declarationGroup the variables declared at the start
 * @property initializations the expressions initialized at the start
 * @property updates the expressions evaluated on each iteration
 * @property condition the optional loop condition
 * @param body the statement being looped
 * @param loc the location in the source file
 */
class ForLoopStatement(
  val declarationGroup: VariableDeclarationGroup?,
  val initializations: List<Expression>,
  val updates: List<Expression>,
  val condition: Expression?,
  body: Statement,
  loc: SourceLocation
) : LoopStatement(body, loc) {
  override fun getChildren(): List<Node> =
    initializations + listOfNotNull(declarationGroup, condition, body) + updates
}

/**
 * A enhanced `for` loop statement.
 *
 * @property elementDeclarations the element iterator
 * @property collection the collection to iterate on
 * @param body the statement being looped
 * @param loc the location in the source file
 */
class EnhancedForLoopStatement(
  val elementDeclarations: VariableDeclarationGroup,
  val collection: Expression,
  body: Statement,
  loc: SourceLocation
) : LoopStatement(body, loc) {
  override fun getChildren(): List<Node> = listOf(elementDeclarations, collection, body)
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
