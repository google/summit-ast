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

/**
 * A compound statement.
 *
 * This is a block of multiple statements, combined into the syntactic role of a single statement.
 *
 * In the most typical case (a block enclosed by curly braces), the compound statement is also a
 * scoping boundary: variables declared inside are not visible outside of the subtree. In other
 * circumstances (such as "lowering" syntax or transforming the AST), a scoping boundary may not be
 * appropriate.
 *
 * @property statements the list of statements
 * @property scoping whether this is a scoping boundary
 * @param loc the location in the source file
 */
class CompoundStatement(
  val statements: List<Statement>,
  val scoping: Scoping,
  loc: SourceLocation
) : Statement(loc) {
  /** This determines whether symbols declared inside are visible externally. */
  enum class Scoping {
    SCOPE_BOUNDARY,
    SCOPE_TRANSPARENT,
  }

  override fun getChildren(): List<Node> = statements
}
