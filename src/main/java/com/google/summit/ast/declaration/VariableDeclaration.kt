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

package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.expression.Expression
import com.google.summit.ast.modifier.Modifier

/**
 * A declaration for a local variable.
 *
 * This includes explicit declarations but also (for example) `for` loops.
 *
 * @param id the name of the variable
 * @property type a reference to the type of the variable
 * @property modifiers a list of any modifiers
 * @property initializer an optional initializer expression
 * @param sourceLocation the location in the source file
 */
class VariableDeclaration(
  id: Identifier,
  val type: TypeRef,
  modifiers: List<Modifier>,
  val initializer: Expression?,
  loc: SourceLocation
) : Declaration(id, modifiers, loc) {

  override fun getChildren(): List<Node> = modifiers + listOfNotNull(id, type, initializer)
}
