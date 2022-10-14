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

/**
 * A `try` statement.
 *
 * @property body the statement to `try`
 * @property catchBlocks the `catch` blocks
 * @property finallyBlocks the optional `finally` block
 * @param loc the location in the source file
 */
class TryStatement(
  val catchBlocks: List<CatchBlock>,
  val body: Statement,
  val finallyBlock: Statement?,
  loc: SourceLocation
) : Statement(loc) {
  override fun getChildren(): List<Node> = catchBlocks + listOfNotNull(body, finallyBlock)

  /**
   * A `catch` block.
   *
   * @property exceptionDeclarations the exception variable declaration
   * @property body the statement to execute when caught
   * @param loc the location in the source file
   */
  class CatchBlock(
    val exceptionDeclarations: VariableDeclarationGroup,
    val body: Statement,
    loc: SourceLocation
  ) : Statement(loc) {
    override fun getChildren(): List<Node> = listOf(exceptionDeclarations, body)
  }
}
