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

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.expression.Expression

/**
 * An outer and a base class for any kind of DML statement.
 *
 * @property value the DML operand
 * @param loc the location in the source file
 */
sealed class DmlStatement(val value: Expression, loc: SourceLocation) : Statement(loc) {
  override fun getChildren(): List<Node> = listOfNotNull(value)

  /**
   * A DML insert statement.
   *
   * @param value the DML operand
   * @param loc the location in the source file
   */
  class Insert(value: Expression, loc: SourceLocation) : DmlStatement(value, loc)

  /**
   * A DML update statement.
   *
   * @param value the DML operand
   * @param loc the location in the source file
   */
  class Update(value: Expression, loc: SourceLocation) : DmlStatement(value, loc)

  /**
   * A DML delete statement.
   *
   * @param value the DML operand
   * @param loc the location in the source file
   */
  class Delete(value: Expression, loc: SourceLocation) : DmlStatement(value, loc)

  /**
   * A DML undelete statement.
   *
   * @param value the DML operand
   * @param loc the location in the source file
   */
  class Undelete(value: Expression, loc: SourceLocation) : DmlStatement(value, loc)

  /**
   * A DML upsert statement.
   *
   * @param value the DML operand
   * @param name the optional field name
   * @param loc the location in the source file
   */
  class Upsert(value: Expression, val name: Identifier?, loc: SourceLocation) :
    DmlStatement(value, loc) {
    override fun getChildren(): List<Node> = listOfNotNull(value, name)
  }

  /**
   * A DML merge statement.
   *
   * @param value the target value to be updated
   * @param from the value to be merged from
   * @param loc the location in the source file
   */
  class Merge(value: Expression, val from: Expression, loc: SourceLocation) :
    DmlStatement(value, loc) {
    override fun getChildren(): List<Node> = listOfNotNull(value, from)
  }
}
