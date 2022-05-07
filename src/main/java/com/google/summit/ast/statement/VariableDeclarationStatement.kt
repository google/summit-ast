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
import com.google.summit.ast.declaration.VariableDeclaration

/**
 * A variable declaration statement.
 *
 * @property variableDeclarations the declared variables
 * @param loc the location in the source file
 */
class VariableDeclarationStatement(
  val variableDeclarations: List<VariableDeclaration>,
  loc: SourceLocation
) : Statement(loc) {

  override fun getChildren(): List<Node> = variableDeclarations
}
